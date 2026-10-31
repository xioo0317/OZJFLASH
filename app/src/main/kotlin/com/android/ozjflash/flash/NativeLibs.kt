package com.android.ozjflash.flash

import android.content.Context
import java.io.File

/**
 * 原生库装载与管理。
 *
 * - prepare(): 把打包的「改名可执行二进制」(libadb.so / libfastboot.so / libbash.so ...)
 *   从应用原生库目录释放到 files/bin/ 并加可执行权限，返回 bin 目录路径，
 *   供终端把该路径注入 PATH —— 这样终端内可直接执行 fastboot / adb / bash 等刷写工具。
 * - loadJni(): 按需加载 JNI 共享库（com.Android.MoFlash.data.* 等桥接类使用）。
 */
object NativeLibs {
    private val EXEC_MAP = mapOf(
        "libadb.so" to "adb",
        "libfastboot.so" to "fastboot",
        "libbash.so" to "bash",
        "lib7zz.so" to "7z",
        "libgrep.so" to "grep",
        "libsimg2img.so" to "simg2img",
        "liblpmake.so" to "lpmake"
    )

    private val JNI_LIBS = listOf(
        "termux", "fdpass", "nativecmd", "qdlargs", "miunlock", "mtkcmd",
        "cmdgen", "secrets", "ksud", "antumbra", "payload-dumper-go", "patch_sn", "opscrypto"
    )

    /** 释放可执行二进制到 files/bin，返回其绝对路径（用于终端 PATH） */
    fun prepare(context: Context): String {
        val libDir = File(context.applicationInfo.nativeLibraryDir)
        val binDir = File(context.filesDir, "bin").apply { mkdirs() }
        for ((src, dst) in EXEC_MAP) {
            val inFile = File(libDir, src)
            if (inFile.exists()) {
                val out = File(binDir, dst)
                inFile.copyTo(out, overwrite = true)
                out.setExecutable(true, false)
            }
        }
        return binDir.absolutePath
    }

    /** 按需加载 JNI 原生库（单个缺失不致命，静默跳过） */
    fun loadJni() {
        for (lib in JNI_LIBS) {
            try { System.loadLibrary(lib) } catch (_: Throwable) {}
        }
    }

    /** 选择终端默认 shell：优先内置 bash，否则回退系统 sh */
    fun shellPath(binPath: String): String {
        val bash = File(binPath, "bash")
        return if (bash.exists()) bash.absolutePath else (System.getenv("SHELL") ?: "/system/bin/sh")
    }
}
