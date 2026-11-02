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
 *      符号链接被拒，仍可用 libozj_xxx.so 全名调用。
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

    /** 个别工具名与真实二进制不同：adb 本体就是 tadb（原包里的 adb 只是调用 tadb 的脚本） */
    private val ALIAS = mapOf("adb" to "tadb")

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

    /** 工具名 -> jniLibs 中的文件名 */
    fun libName(tool: String): String = "libozj_${tool.replace('-', '_')}.so"

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

    /** PATH：filesDir/bin（干净工具名）→ nativeLibraryDir（libozj_*.so）→ 系统路径 */
    fun pathEnv(binPath: String, nativeDir: String? = null): String {
        val sys = System.getenv("PATH")
        val sysPath = if (sys.isNullOrEmpty())
            "/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin:/sbin"
        else sys
        return listOf(binPath, nativeDir, sysPath).filter { !it.isNullOrEmpty() }.joinToString(":")
    }

    /** 终端进程环境变量 */
    fun shellEnv(context: Context, binPath: String, nativeDir: String? = null): Map<String, String> = mapOf(
        "PATH" to pathEnv(binPath, nativeDir),
        "HOME" to context.filesDir.absolutePath,
        "TMPDIR" to context.cacheDir.absolutePath,
        "TERM" to "xterm-256color",
        "LANG" to "en_US.UTF-8"
    )

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
