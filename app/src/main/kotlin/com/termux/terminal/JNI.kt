package com.termux.terminal

import kotlin.jvm.JvmField

/**
 * 桥接打包的 libtermux.so（Termux terminal-emulator 的 JNI 层）。
 * 包名 / 类名 / 方法名必须与 libtermux.so 导出的 Java_com_termux_terminal_JNI_* 符号一致。
 * 仅 PTY 模式 (TerminalEngine.USE_TERMUX_PTY = true) 使用。
 */
object JNI {
    @JvmField var mPtyFd: Int = -1

    private var loaded = false
    fun load() {
        if (!loaded) {
            System.loadLibrary("termux")
            loaded = true
        }
    }

    external fun createSubprocess(
        executable: String,
        arguments: Array<String>?,
        environment: Array<String>?,
        workingDirectory: String,
        columns: Int,
        rows: Int
    ): Int

    external fun setPtyWindowSize(fd: Int, columns: Int, rows: Int)
    external fun setPtyUTF8Mode(fd: Int)
    external fun close(fd: Int)
    external fun waitFor(fd: Int): Int
}
