package com.android.ozjflash.ui.screen.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.android.ozjflash.terminal.TerminalEngine
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode

/** 输出缓冲上限，避免长时间刷写日志吃满内存 */
private const val MAX_OUTPUT_CHARS = 128_000

/**
 * 终端页面的状态容器：把 TerminalEngine 的输出流累积成字符串，并提供输入 / 清屏 / 快捷指令。
 * 两个 UI 主体（Miuix / Material）共用同一份状态。
 */
class TerminalConsole internal constructor(
    val engine: TerminalEngine,
    val binDir: String,
) {
    var output: String by mutableStateOf("")
        internal set
    var input: String by mutableStateOf("")
        internal set

    fun onInput(value: String) { input = value }

    fun submit(line: String = input) {
        if (line.isBlank()) {
            input = ""
            return
        }
        engine.writeLine(line)
        input = ""
    }

    fun sendCtrlC() = engine.sendCtrlC()
    fun clear() { output = "" }
    fun runCheckScript() = submit("sh $binDir/check_env.sh")

    internal fun append(chunk: String) {
        val next = output + chunk
        output = if (next.length > MAX_OUTPUT_CHARS) next.takeLast(MAX_OUTPUT_CHARS) else next
    }
}

@Composable
fun rememberTerminalConsole(engine: TerminalEngine, binDir: String): TerminalConsole {
    val console = remember(engine, binDir) { TerminalConsole(engine, binDir) }
    LaunchedEffect(engine) {
        engine.output.collect { chunk -> console.append(chunk) }
    }
    return console
}

/** 快捷指令表，两套主体共用，只是渲染方式不同 */
internal data class QuickAction(val label: String, val run: () -> Unit)

@Composable
internal fun rememberQuickActions(console: TerminalConsole): List<QuickAction> = remember(console) {
    listOf(
        QuickAction("clear") { console.clear() },
        QuickAction("Ctrl+C") { console.sendCtrlC() },
        QuickAction("check_env.sh") { console.runCheckScript() },
        QuickAction("fastboot devices") { console.submit("fastboot devices") },
        QuickAction("getvar all") { console.submit("fastboot getvar all") },
        QuickAction("adb devices") { console.submit("adb devices") },
    )
}

@Composable
fun TerminalPager(
    console: TerminalConsole,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> TerminalMiuix(console, bottomInnerPadding, isCurrentPage)
        UiMode.Material -> TerminalMaterial(console, bottomInnerPadding, isCurrentPage)
    }
}
