package com.android.ozjflash.flash

import android.content.Context
import android.os.Build
import android.system.Os
import java.io.File
import java.nio.file.Files

/**
 * 内置刷写工具的装载与管理。
 *
 * 关键约束：Android 10（API 29）起，targetSdk >= 29 的应用**不能** exec 位于
 * 应用私有目录（/data/data/<pkg>/...）的文件，内核直接返回 EACCES。
 * 之前把 30MB 二进制复制到 filesDir 的做法在 Android 10+ 上全部不可执行，
 * 表现为「脚本一执行就退出 / 工具 Permission denied」。现在改为：
 *
 *   1. 打包时把每个工具改名成 lib*.so 放进 jniLibs，由系统安装到
 *      nativeLibraryDir（/data/app/.../lib/arm64），该目录允许执行；
 *   2. 运行时在 filesDir/bin 为每个工具创建**符号链接**，链接名即工具名
 *      （adb / fastboot ...）。execve 解析链接后按目标路径做权限检查，
 *      目标在 nativeLibraryDir，因此可以正常执行；
 *   3. PATH 同时纳入 filesDir/bin 与 nativeLibraryDir：万一某个 ROM 上
 *      符号链接被拒，仍可用 libxxx.so 全名调用。
 *
 * 另外 prepare() 只建链接、不再复制几十 MB 资产，启动不再卡主线程；
 * probeExec() 做一次真实执行探测，结果写入 files/env.log，便于在设备上
 * 直接判断工具链是否真的可用。
 */
object NativeLibs {

    /** 内置工具名（与自检脚本 command -v 的检验项一致） */
    val TOOLS = listOf(
        "adb", "fastboot", "tadb",
        "lpmake", "lpunpack", "simg2img", "img2simg",
        "mke2fs", "e2fsdroid",
        "make_f2fs", "make_f2fs_casefold", "sload_f2fs",
        "payload-dumper-go", "magiskboot", "libksud",
        "sqlite3", "etc1tool", "hprof-conv"
    )

    /**
     * 无别名：adb 直接用 libadb.so。
     *
     * 原来的 libtadb.so **不支持** MOMIUI_USB_SOCK，免 root 下无法接收
     * 已授权的 USB fd，命令行永远是 `< waiting for any device >`；
     * 现改用 MOFLASH 的魔改版 adb（libadb.so），与 fastboot 同一套 fd 直通机制。
     * tadb 仍保留在 TOOLS 中，供终端里普通使用。
     */
    private val ALIAS = emptyMap<String, String>()

    /** jniLibs 里以 .so 形式分发的辅助可执行（bash / 7z / grep / qdl） */
    private val EXEC_FROM_LIBS = mapOf(
        "libbash.so" to "bash",
        "lib7zz.so" to "7z",
        "libgrep.so" to "grep",
        "libqdl.so" to "qdl"
    )

    /** 需要 System.loadLibrary 的 JNI 桥接库（缺失时静默跳过） */
    private val JNI_LIBS = listOf(
        "termux", "fdpass", "nativecmd", "qdlargs", "miunlock", "mtkcmd",
        "cmdgen", "secrets", "antumbra", "patch_sn", "opscrypto"
    )

    /**
     * 工具名 -> jniLibs 中的文件名。
     *
     * 统一采用「命令名格式」：`lib<工具名>.so`，不再带 `ozj_` 厂商前缀
     * （libmagiskboot.so / libfastboot.so / libpayload_dumper_go.so …）。
     * 工具名本身已以 lib 开头时（如 libksud）不重复拼前缀，保持 libksud.so。
     */
    fun libName(tool: String): String {
        val name = tool.replace('-', '_')
        return if (name.startsWith("lib")) "$name.so" else "lib$name.so"
    }

    /** 建立工具链接，返回 bin 目录绝对路径 */
    fun prepare(context: Context): String {
        val binDir = File(context.filesDir, "bin").apply { mkdirs() }
        val nativeDir = context.applicationInfo.nativeLibraryDir?.let { File(it) }

        val report = StringBuilder()
        report.appendLine("nativeLibraryDir = ${nativeDir?.absolutePath}")
        report.appendLine("filesDir/bin     = ${binDir.absolutePath}")
        report.appendLine("SDK_INT          = ${Build.VERSION.SDK_INT}")

        for (name in TOOLS) link(report, name, nativeDir, binDir)
        if (nativeDir != null) {
            for ((src, dst) in EXEC_FROM_LIBS) link(report, dst, nativeDir, binDir, src)
        }
        extractScript(context, binDir)

        val probe = probeExec(binDir.absolutePath)
        report.appendLine()
        report.appendLine("probe fastboot --version : $probe")
        runCatching { File(context.filesDir, "env.log").writeText(report.toString()) }

        return binDir.absolutePath
    }

    private fun link(
        report: StringBuilder,
        name: String,
        nativeDir: File?,
        binDir: File,
        libFile: String? = null,
    ) {
        if (nativeDir == null) {
            report.appendLine("[×] $name : 无 nativeLibraryDir")
            return
        }
        val fileName = libFile ?: libName(ALIAS[name] ?: name)
        val target = File(nativeDir, fileName)
        if (!target.exists()) {
            report.appendLine("[×] $name : 缺少 $fileName")
            return
        }
        val ok = replaceLink(File(binDir, name), target)
        runCatching { Os.chmod(target.absolutePath, 0b111101101) }
        report.appendLine(
            if (ok) "[√] $name -> ${target.absolutePath}"
            else "[!] $name : 链接失败，已回退复制（可能不可执行）"
        )
    }

    /** 用符号链接指向真实二进制；ROM 不支持时回退为复制 */
    private fun replaceLink(link: File, target: File): Boolean {
        try {
            if (link.exists() || Files.isSymbolicLink(link.toPath())) link.delete()
        } catch (_: Throwable) { }
        try {
            Os.symlink(target.absolutePath, link.absolutePath)
            return true
        } catch (_: Throwable) { }
        return try {
            target.copyTo(link, overwrite = true)
            chmodExec(link.absolutePath)
            false
        } catch (_: Throwable) { false }
    }

    /** 释放内置脚本（脚本由 sh 解释执行，不依赖 exec 位） */
    private fun extractScript(context: Context, binDir: File) {
        for (name in listOf("check_env.sh")) {
            runCatching {
                context.assets.open("scripts/$name").use { input ->
                    File(binDir, name).outputStream().use { input.copyTo(it) }
                }
            }
        }
    }

    private fun chmodExec(path: String) {
        try {
            Os.chmod(path, 0b111101101) // 0755
        } catch (_: Throwable) {
            try { File(path).setExecutable(true, false) } catch (_: Throwable) { }
        }
    }

    /** PATH：filesDir/bin（干净工具名）→ nativeLibraryDir（lib*.so）→ 系统路径 */
    fun pathEnv(binPath: String, nativeDir: String? = null): String {
        val sys = System.getenv("PATH")
        val sysPath = if (sys.isNullOrEmpty())
            "/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin:/sbin"
        else sys
        return listOf(binPath, nativeDir, sysPath).filter { !it.isNullOrEmpty() }.joinToString(":")
    }

    /**
     * 终端工作目录 / HOME。
     *
     * 之前直接用 filesDir（/data/data/<pkg>/files/home）。在多数 ROM 上，应用 UID 对
     * /data/data 只有「执行(x)」、没有「读(r)」权限；bash 启动做 getcwd() 时若需要解析
     * 整条路径就会吃 EACCES，报
     *   shell-init: error retrieving current directory: getcwd: Permission denied
     * 这就是终端「访问不了 APP 私有目录」的根因（并非上游 UID 残留，而是 /data/data/<pkg>
     * 的祖先目录权限让 getcwd 失败）。
     *
     * 根治：HOME/cwd 放到 /data/local/tmp/<pkg>，其祖先 /data、/data/local、/data/local/tmp
     * 均为 755/1777，任何 UID 都能无障碍 traverse + read，getcwd 永不再报错。/data/local/tmp
     * 不可写时再回退到应用私有 cache（应用 UID 对其有完整权限，getcwd 同样可用）。
     * 注意：OZJ_BIN 等工具链路径仍指向 files/bin，与 HOME 无关，不受此改动影响。
     */
    fun homeDir(context: Context): File {
        val pkg = runCatching { context.packageName }.getOrDefault("ozjflash")
        val tmp = File("/data/local/tmp", pkg)
        if ((tmp.exists() && tmp.isDirectory) || runCatching { tmp.mkdirs() }.getOrDefault(false)) {
            // 0700：仅本 UID 可读写，避免其它应用翻看终端历史
            runCatching { Os.chmod(tmp.absolutePath, 0b111000000) }
            return tmp
        }
        // 兜底：应用私有 cache（/data/data/<pkg>/cache，应用 UID 必有完整权限）
        return File(context.cacheDir, "home").apply { mkdirs() }
    }

    /** 终端进程环境变量 */
    fun shellEnv(context: Context, binPath: String, nativeDir: String? = null): Map<String, String> {
        val home = homeDir(context)
        return mapOf(
            "PATH" to pathEnv(binPath, nativeDir),
            "HOME" to home.absolutePath,
            "TMPDIR" to context.cacheDir.absolutePath,
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8",
            // mksh（/system/bin/sh）读 $ENV 作为启动脚本
            "ENV" to File(home, ".mkshrc").absolutePath,
            // 给 rc 里展示工具链路径用
            "OZJ_BIN" to binPath,
        )
    }

    /**
     * 准备交互式 shell 的启动脚本。
     *
     * - 我们自己维护一份 `.ozjflash.rc`（每次启动重写，仅设置提示符与初始目录），不污染用户配置；
     * - 在用户的 `.bashrc` / `.mkshrc` 末尾幂等地挂一行 source（带 marker，不重复追加）。
     * - 初始工作目录用 homeDir()（/data/local/tmp/<pkg>，祖先目录可无障碍遍历），保证 bash
     *   启动做 getcwd() 不会因 /data/data 祖先目录无读权限而报
     *   "shell-init: error retrieving current directory: getcwd: cannot access parent directories"。
     *   随后在 rc 里 cd 到应用私有目录 /data/data/<pkg>，使 `ls` 默认落在应用私有目录、提示符显示
     *   `:/data/data/<pkg> #`，同时不再打印任何横幅。
     */
    fun prepareShell(context: Context, binPath: String): File {
        val home = homeDir(context)
        val dataDir = runCatching { context.dataDir?.absolutePath }.getOrDefault("/data/data/com.android.ozjflash")
        val rc = listOf(
            "# 由 OZJFLASH 自动生成，请勿手改",
            "case \"\$-\" in *i*) ;; *) return 0 2>/dev/null || exit 0 ;; esac",
            "export PS1=':\\w # '",
            "cd '$dataDir' 2>/dev/null || true",
        ).joinToString("\n") + "\n"
        File(home, ".ozjflash.rc").writeText(rc)

        val marker = "# OZJFLASH-RC"
        val sourceLine = "$marker\n[ -f \"\$HOME/.ozjflash.rc\" ] && . \"\$HOME/.ozjflash.rc\"\n"
        for (name in listOf(".bashrc", ".mkshrc")) {
            val f = File(home, name)
            val text = if (f.exists()) f.readText() else ""
            if (!text.contains(marker)) {
                val lead = if (text.isEmpty() || text.endsWith("\n")) "" else "\n"
                f.appendText(lead + sourceLine)
            }
        }
        return home
    }

    /** 选择终端 shell：优先自带 bash，否则回退 /system/bin/sh */
    fun shellPath(binPath: String): String {
        val bash = File(binPath, "bash")
        return if (bash.canExecute()) bash.absolutePath else "/system/bin/sh"
    }

    /** 依据 shebang 生成执行脚本的命令（保留给需要交互解释的场景） */
    fun scriptCommand(script: File): String {
        val shebang = readShebang(script)
        val interpreter = when {
            shebang.isNullOrBlank() -> "/system/bin/sh"
            else -> shebang.trim()
        }
        return "'%s' '%s'".format(interpreter, script.absolutePath)
    }

    /** 脚本不一定有 exec 位，统一用 sh 显式调用最稳 */
    fun runScriptCommand(script: File): String = "'/system/bin/sh' '%s'".format(script.absolutePath)

    private fun readShebang(script: File): String? = try {
        script.bufferedReader().useLines { lines ->
            val first = lines.firstOrNull() ?: return@useLines null
            val s = first.trim()
            if (s.startsWith("#!")) s.removePrefix("#!").trim().takeIf { it.isNotEmpty() } else null
        }
    } catch (_: Throwable) { null }

    /** 缺失的工具（filesDir/bin 下没有链接或链接已断裂） */
    fun missingTools(binPath: String): List<String> =
        TOOLS.filter { !File(binPath, it).exists() }

    /** 真实执行一次工具，确认「能不能 exec」，返回给 UI/日志看的结论 */
    fun probeExec(binPath: String, tool: String = "fastboot"): String = try {
        val p = ProcessBuilder(File(binPath, tool).absolutePath, "--version")
            .redirectErrorStream(true)
            .start()
        val out = p.inputStream.bufferedReader().readText().trim().lineSequence().firstOrNull().orEmpty()
        val code = p.waitFor()
        "exit=$code $out"
    } catch (t: Throwable) {
        "FAIL ${t.javaClass.simpleName}: ${t.message}"
    }

    /** 按需加载 JNI 原生库（单个缺失不致命，静默跳过） */
    fun loadJni() {
        for (lib in JNI_LIBS) {
            try { System.loadLibrary(lib) } catch (_: Throwable) { }
        }
    }
}
