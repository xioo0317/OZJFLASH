package com.android.ozjflash.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

/**
 * 工具页日志控制台引擎（进程管道模式）。
 *
 * 用 ProcessBuilder 拉起本地 shell，并把 NativeLibs.prepare() 释放的
 * 刷写工具（adb / fastboot / bash / ...）注入 PATH，供工具页执行脚本并回显输出。
 *
 * 终端页已升级为真终端（见 [RealTerminal]，Termux 上游 PTY + ANSI 渲染），
 * 本类仅服务于工具页的脚本执行场景。
 */
class TerminalEngine(
    private val scope: CoroutineScope,
    private val shellPath: String,
    private val extraPath: String? = null
) {
    private var process: Process? = null
    private var stdin: java.io.OutputStream? = null

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 4096)
    val output = _output.asSharedFlow()

    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    fun start() {
        if (_running.value) return
        try {
            val pb = ProcessBuilder(shellPath)
            pb.redirectErrorStream(true)
            pb.environment().apply {
                if (extraPath != null) put("PATH", "$extraPath:${get("PATH") ?: ""}")
                put("TERM", "xterm-256color")
            }
            val p = pb.start()
            process = p
            stdin = p.outputStream
            _running.value = true
            scope.launch(Dispatchers.IO) { pump(p.inputStream) }
            scope.launch(Dispatchers.IO) {
                val code = p.waitFor()
                _running.value = false
                _output.emit("\r\n[进程结束 exit=$code]\r\n")
            }
        } catch (t: Throwable) {
            _running.value = false
            scope.launch { _output.emit("\r\n[终端启动失败] ${t.message}\r\n") }
        }
    }

    private suspend fun pump(input: InputStream) {
        val buf = ByteArray(4096)
        try {
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                _output.emit(String(buf, 0, n, Charsets.UTF_8))
            }
        } catch (_: Throwable) { }
    }

    fun write(text: String) {
        try { stdin?.write(text.toByteArray(Charsets.UTF_8)); stdin?.flush() } catch (_: Throwable) {}
    }
    fun writeLine(line: String) = write(line + "\n")
    fun sendCtrlC() = write("\u0003")
    fun destroy() {
        try { process?.destroy() } catch (_: Throwable) {}
        _running.value = false
    }
}
