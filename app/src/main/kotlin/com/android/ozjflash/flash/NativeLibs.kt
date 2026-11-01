package com.android.ozjflash.flash

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.system.Os
import java.io.File

/**
 * 内置终端工具包装载与管理。
 *
 * 工具来源：assets/bin/（AOSP 平台工具 / f2fs-tools / e2fsprogs / libsparse /
 * liblp / magiskboot / ksud / payload-dumper-go 等，AArch64 静态可执行文件），
 * 外加 jniLibs 里少量改名可执行文件（bash / 7z / grep / qdl）。
 *
 * - prepare() : 释放资产到 files/bin 并 chmod 755，返回 bin 目录路径
 * - pathEnv() : 组装 PATH（自带 bin 优先，同时保留系统路径以便使用 toybox 的 tar/gzip）
 * - shellEnv(): 终端进程环境变量（PATH / HOME / TMPDIR / TERM）
 * - resolveScriptRunner(): 按 shebang 决定解释器，用于执行 .sh 脚本
 */
object NativeLibs {

    /** assets/bin 下提供的工具名（对齐检测脚本的 command -v 检查项） */
    val TOOLS = listOf(
        "adb", "fastboot", "tadb",
        "lpmake", "lpunpack", "simg2img", "img2simg",
        "mke2fs", "e2fsdroid",
        "make_f2fs", "make_f2fs_casefold", "sload_f2fs",
        "payload-dumper-go", "magiskboot", "libksud",
        "sqlite3", "etc1tool", "hprof-conv"
    )

    /** jniLibs 中仍需释放到 PATH 的辅助可执行二进制 */
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

    /** 释放全部内置工具到 files/bin，返回 bin 目录绝对路径 */
    fun prepare(context: Context): String {
        val binDir = File(context.filesDir, "bin").apply { mkdirs() }
        val stamp = File(binDir, ".stamp")
        val stampValue = "${TOOLS.size}-${appVersion(context)}"

        // 资产近 30MB，版本未变化时跳过重复释放
        if (!stamp.exists() || stamp.readText().trim() != stampValue) {
            copyAssetDir(context, "bin", binDir)
            stamp.writeText(stampValue)
        }
        copyExecFromLibs(context, binDir)
        extractScript(context, binDir)

        // 每次启动都兜底 chmod（部分 ROM/备份恢复后可执行位会丢失）
        binDir.listFiles()?.forEach { f ->
            if (f.isFile && !f.name.startsWith(".")) chmodExec(f.absolutePath)
        }
        return binDir.absolutePath
    }

    private fun appVersion(context: Context): Long {
        val pi: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else pi.versionCode.toLong()
    }

    private fun copyAssetDir(context: Context, assetPath: String, outDir: File) {
        val names = context.assets.list(assetPath) ?: return
        outDir.mkdirs()
        for (name in names) {
            val sub = "$assetPath/$name"
            val children = context.assets.list(sub)
            if (!children.isNullOrEmpty()) {
                copyAssetDir(context, sub, File(outDir, name))
            } else {
                context.assets.open(sub).use { input ->
                    val out = File(outDir, name)
                    out.outputStream().use { input.copyTo(it) }
                    chmodExec(out.absolutePath)
                }
            }
        }
    }

    private fun copyExecFromLibs(context: Context, binDir: File) {
        context.applicationInfo.nativeLibraryDir ?: return
        val libDir = File(context.applicationInfo.nativeLibraryDir)
        for ((src, dst) in EXEC_FROM_LIBS) {
            val f = File(libDir, src)
            if (f.exists()) {
                val out = File(binDir, dst)
                f.copyTo(out, overwrite = true)
                chmodExec(out.absolutePath)
            }
        }
    }

    /** 把内置的检测脚本也释放出来，方便终端里 sh 直接执行 */
    private fun extractScript(context: Context, binDir: File) {
        listOf("check_env.sh").forEach { name ->
            try {
                context.assets.open("scripts/$name").use { input ->
                    val out = File(binDir, name)
                    out.outputStream().use { input.copyTo(it) }
                    chmodExec(out.absolutePath)
                }
            } catch (_: Throwable) { }
        }
    }

    private fun chmodExec(path: String) {
        try {
            Os.chmod(path, 0b111101101) // 0755
        } catch (_: Throwable) {
            try { File(path).setExecutable(true, false) } catch (_: Throwable) { }
        }
    }

    /** 组装 PATH：自带 bin 优先，随后是系统路径（保证 sh/toybox/tar/gzip 可用） */
    fun pathEnv(binPath: String): String {
        val sys = System.getenv("PATH")
        val sysPath = if (sys.isNullOrEmpty())
            "/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin:/sbin"
        else sys
        return "$binPath:$sysPath"
    }

    /** 终端进程环境变量 */
    fun shellEnv(context: Context, binPath: String): Map<String, String> = mapOf(
        "PATH" to pathEnv(binPath),
        "HOME" to context.filesDir.absolutePath,
        "TMPDIR" to context.cacheDir.absolutePath,
        "TERM" to "xterm-256color",
        "LANG" to "en_US.UTF-8"
    )

    /** 选择终端 shell：优先内置 bash，否则回退 /system/bin/sh */
    fun shellPath(binPath: String): String {
        val bash = File(binPath, "bash")
        return if (bash.canExecute()) bash.absolutePath else "/system/bin/sh"
    }

    /**
     * 依据 shebang 生成执行脚本的命令；没有 shebang 则用系统 sh 兜底。
     * 注意这里返回的是「给人看的命令行」（写进终端），实际由 shell 解释执行。
     */
    fun scriptCommand(script: File): String {
        val shebang = readShebang(script)
        val interpreter = when {
            shebang.isNullOrBlank() -> "/system/bin/sh"
            else -> shebang.trim()
        }
        return "'%s' '%s'".format(interpreter, script.absolutePath)
    }

    private fun readShebang(script: File): String? = try {
        script.bufferedReader().useLines { lines ->
            val first = lines.firstOrNull() ?: return@useLines null
            val s = first.trim()
            if (s.startsWith("#!")) s.removePrefix("#!").trim().takeIf { it.isNotEmpty() } else null
        }
    } catch (_: Throwable) { null }

    /** 当前缺失的工具列表（用于 UI 自检展示） */
    fun missingTools(binPath: String): List<String> =
        TOOLS.filter { !File(binPath, it).exists() }

    /** 按需加载 JNI 原生库（单个缺失不致命，静默跳过） */
    fun loadJni() {
        for (lib in JNI_LIBS) {
            try { System.loadLibrary(lib) } catch (_: Throwable) { }
        }
    }
}
