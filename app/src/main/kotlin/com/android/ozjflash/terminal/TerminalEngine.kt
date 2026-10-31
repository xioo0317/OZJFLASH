package com.android.ozjflash.terminal

import com.termux.terminal.JNI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * 内置终端引擎。
 *
 * 默认 PROCESS 模式：用 ProcessBuilder 拉起本地 shell（沙盒内即可运行，无需 root），
 * 并把 NativeLibs.prepare() 释放的刷写工具（adb / fastboot / bash / ...）注入 PATH，
 * 使终端内可直接执行刷写指令。
 *
 * PTY 模式（USE_TERMUX_PTY = true）：改用打包的 libtermux.so 提供真正 PTY 终端
 * （com.termux.terminal.JNI.createSubprocess），交互式体验更完整。
 */
class TerminalEngine(
    private val scope: CoroutineScope,
    private val shellPath: String,
    private val extraPath: String? = null
) {
    private var process: Process? = null
    private var pty: TermuxPty? = null
    private var stdin: OutputStream? = null

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 4096)
    val output = _output.asSharedFlow()

    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    fun start() {
        if (_running.value) return
        try {
            if (USE_TERMUX_PTY) startPty() else startProcess()
        } catch (t: Throwable) {
            // 任何异常回退到普通进程 shell
            if (pty == null) {
                try { startProcess() } catch (t2: Throwable) {
                    _running.value = false
                    scope.launch { _output.emit("\r\n[终端启动失败] ${t2.message}\r\n") }
                }
            }
        }
    }

    private fun startProcess() {
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
    }

    private fun startPty() {
        val jni = JNI
        jni.load()
        jni.createSubprocess(
            shellPath,
            arrayOf("-i"),
            arrayOf("TERM=xterm-256color", "PATH=${extraPath ?: ""}:${System.getenv("PATH") ?: ""}"),
            "/",
            100, 30
        )
        val p = TermuxPty(jni.mPtyFd, jni)
        pty = p
        stdin = p.outputStream
        _running.value = true
        scope.launch(Dispatchers.IO) { p.pump(_output) }
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
    fun resize(columns: Int, rows: Int) { pty?.resize(columns, rows) }
    fun destroy() {
        try { process?.destroy() } catch (_: Throwable) {}
        try { pty?.close() } catch (_: Throwable) {}
        _running.value = false
    }

    companion object {
        /** 默认 false：稳定可靠的进程 shell；置 true 启用打包的 Termux PTY 终端 */
        var USE_TERMUX_PTY = false
    }
}

/** 用反射把 pty master fd 包装成 FileDescriptor，从而用标准流读写（Termux 同款做法） */
private class TermuxPty(fd: Int, private val jni: JNI) {
    private val fdObj: FileDescriptor = run {
        val d = FileDescriptor()
        val f = FileDescriptor::class.java.getDeclaredField("descriptor")
        f.isAccessible = true
        f.set(d, fd)
        d
    }
    val outputStream: OutputStream = FileOutputStream(fdObj)
    private val fis = FileInputStream(fdObj)

    suspend fun pump(out: MutableSharedFlow<String>) {
        val buf = ByteArray(4096)
        try {
            while (true) {
                val n = fis.read(buf)
                if (n < 0) break
                out.emit(String(buf, 0, n, Charsets.UTF_8))
            }
        } catch (_: Throwable) { }
    }
    fun resize(c: Int, r: Int) = jni.setPtyWindowSize(jni.mPtyFd, c, r)
    fun close() {
        try { fis.close() } catch (_: Throwable) {}
        try { outputStream.close() } catch (_: Throwable) {}
        jni.close(jni.mPtyFd)
    }
}
