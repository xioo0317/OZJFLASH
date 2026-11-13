package com.android.ozjflash.flash

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 命令执行日志：诊断信息 + 执行指令 + 执行结果统一写入
 * `filesDir/logs/<功能名>_<时间>.log`。
 *
 * 诊断信息（`info：…` 格式）**不进用户终端**，只落日志文件；工具真实 stdout 由调用方
 * 决定是否同时回显到终端。日志文件名 = 功能名（adb / fastboot）+ 时间戳，便于事后定位。
 */
object CommandLogger {

    private val tsFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /** 开启一次功能日志会话，返回本次日志文件路径 */
    fun begin(context: Context, feature: String): File {
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        val name = feature.filter { it.isLetterOrDigit() || it == '_' }.ifEmpty { "cmd" }
        return File(dir, "${name}_${tsFormat.format(Date())}.log")
    }

    fun append(file: File, line: String) {
        runCatching { file.appendText(line + "\n") }
    }
}
