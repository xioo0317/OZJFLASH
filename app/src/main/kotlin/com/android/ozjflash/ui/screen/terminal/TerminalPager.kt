package com.android.ozjflash.ui.screen.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.terminal.RealTerminal
import com.android.ozjflash.terminal.TerminalEngine
import com.android.ozjflash.terminal.rememberRealTerminal

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
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    TerminalMiuix(terminal, bottomInnerPadding, isCurrentPage)
}

/**
 * 从「设置」入口进入的终端页：自建真终端会话（不依赖主界面常驻会话），
 * 按需求不显示顶栏（返回由系统返回手势 / NavDisplay 处理）。
 */
@Composable
fun TerminalRouteScreen(binPath: String, nativeDir: String?) {
    val shellPath = remember(binPath) { NativeLibs.shellPath(binPath) }
    val realTerminal = rememberRealTerminal(shellPath, binPath, nativeDir)
    DisposableEffect(realTerminal) {
        onDispose { realTerminal.session.finishIfRunning() }
    }
    TerminalPane(terminal = realTerminal, bottomInnerPadding = 0.dp, showTopBar = false)
}