package com.android.ozjflash

import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃捕获。
 *
 * 线刷工具在设备上出问题时最缺的就是堆栈：系统杀进程后 logcat 往往已经滚走。
 * 这里把未捕获异常连同设备信息写入 files/crash.log，下次启动即可在
 * 「设置 → 运行诊断」里查看或复制出来。
 */
object CrashLogger {

    private const val FILE = "crash.log"

    fun install(app: android.app.Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { save(app, thread, throwable) }
            // 交给系统默认处理（弹出停止运行对话框 / 写 tombstone）
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun save(app: android.app.Application, thread: Thread, throwable: Throwable) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val text = buildString {
            appendLine("=== OZJFLASH crash $time ===")
            appendLine("thread   : ${thread.name}")
            appendLine("device   : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("sdk      : ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            appendLine("abi      : ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("nativeLib: ${app.applicationInfo.nativeLibraryDir}")
            appendLine()
            appendLine(throwable.stackTraceToString())
            val cause = throwable.cause
            if (cause != null) {
                appendLine("--- cause ---")
                appendLine(cause.stackTraceToString())
            }
        }
        File(app.filesDir, FILE).appendText(text + "\n")
    }

    /** 最近一次崩溃记录，没有则返回 null */
    fun read(app: android.app.Application): String? =
        File(app.filesDir, FILE).takeIf { it.exists() && it.length() > 0 }?.readText()

    fun clear(app: android.app.Application) {
        runCatching { File(app.filesDir, FILE).delete() }
    }
}
