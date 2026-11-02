package com.android.ozjflash.ui.screen.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.android.ozjflash.terminal.RealTerminal
import com.android.ozjflash.terminal.TerminalEngine
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode

/** 输出缓冲上限，避免长时间刷写日志吃满内存 */
private const val MAX_OUTPUT_CHARS = 128_000

/**
 * 工具页日志控制台的状态容器：把 TerminalEngine 的输出流累积成字符串，
 * 并提供输入 / 清屏 / 快捷指令（工具页执行脚本并回显的场景）。
 *
 * 注意：终端页已升级为真终端（[RealTerminal]，Termux 上游 PTY + ANSI 渲染），
 * 不再使用本类。
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

/**
 * 终端页分派：Miuix / Material 两套主体共用同一个真终端会话（[RealTerminal]），
 * 仅外壳与按钮组件不同。
 */
@Composable
fun TerminalPager(
    terminal: RealTerminal,
    binPath: String,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> TerminalMiuix(terminal, binPath, bottomInnerPadding, isCurrentPage)
        UiMode.Material -> TerminalMaterial(terminal, binPath, bottomInnerPadding, isCurrentPage)
    }
}

/** 真终端快捷命令表，两套主体共用 */
internal data class TerminalCommand(val label: String, val run: () -> Unit)

@Composable
internal fun rememberTerminalCommands(terminal: RealTerminal, binPath: String): List<TerminalCommand> =
    remember(terminal, binPath) {
        listOf(
            TerminalCommand("check_env.sh") { terminal.sendLine("sh '$binPath/check_env.sh'") },
            TerminalCommand("fastboot devices") { terminal.sendLine("fastboot devices") },
            TerminalCommand("fastboot getvar all") { terminal.sendLine("fastboot getvar all") },
            TerminalCommand("adb devices") { terminal.sendLine("adb devices") },
            TerminalCommand("clear") { terminal.sendLine("clear") },
        )
    }
