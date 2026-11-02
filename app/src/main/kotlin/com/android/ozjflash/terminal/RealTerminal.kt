package com.android.ozjflash.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.android.ozjflash.ozjApp
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

/**
 * 真终端（Termux 上游 terminal-emulator / terminal-view 移植）。
 *
 * - 进程：TerminalSession 经 libtermux.so 创建真正 PTY 子进程（bash / system sh）
 * - 渲染：TerminalView 完整 ANSI/VT 解析（光标、颜色、滚回、选择复制）
 * - 输入：软键盘直入 PTY；CTRL / ALT 为一次性粘滞键（与 Termux 行为一致）
 */

/** TerminalSession 只暴露 write(byte[], int, int)，这里统一成单参写入 */
private fun TerminalSession.writeBytes(data: ByteArray) {
    write(data, 0, data.size)
}

class OzjTerminalSessionClient : TerminalSessionClient {

    @Volatile
    var view: TerminalView? = null

    override fun onTextChanged(changedSession: TerminalSession) {
        view?.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) {}

    override fun onSessionFinished(finishedSession: TerminalSession) {
        view?.onScreenUpdated()
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val cm = view?.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("terminal", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val v = view ?: return
        val cm = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(v.context)?.toString() ?: return
        session?.writeBytes(text.toByteArray(Charsets.UTF_8))
    }

    override fun onBell(session: TerminalSession) {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun onColorsChanged(session: TerminalSession) {}

    override fun onTerminalCursorStateChange(state: Boolean) {}

    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

    override fun getTerminalCursorStyle(): Int? = null

    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}
}

/** TerminalView 的交互回调：聚焦/键盘、CTRL-ALT 粘滞键、双指缩放字号 */
class OzjTerminalViewClient : TerminalViewClient {

    lateinit var view: TerminalView

    /** 一次性粘滞键：点击后与下一次按键组合，随后自动复位（Termux 同款行为） */
    var ctrlActive by mutableStateOf(false)
        private set
    var altActive by mutableStateOf(false)
        private set

    fun toggleCtrl() { ctrlActive = !ctrlActive }
    fun toggleAlt() { altActive = !altActive }

    private var textSizeDp = 13

    private val imm: InputMethodManager
        get() = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    override fun onScale(scale: Float): Float {
        if (scale < 0.9f || scale > 1.1f) {
            textSizeDp = (textSizeDp * (if (scale < 1f) 0.95f else 1.05f)).toInt().coerceIn(8, 26)
            view.setTextSize(textSizeDp)
            return 1f
        }
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent?) {
        view.requestFocus()
        imm.showSoftInput(view, 0)
    }

    override fun shouldBackButtonBeMappedToEscape() = false

    override fun shouldEnforceCharBasedInput() = true

    override fun shouldUseCtrlSpaceWorkaround() = true

    override fun isTerminalViewSelected() = view.hasFocus()

    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?) = false

    override fun onKeyUp(keyCode: Int, e: KeyEvent?) = false

    override fun onLongPress(event: MotionEvent?) = false

    override fun readControlKey(): Boolean {
        val active = ctrlActive
        ctrlActive = false
        return active
    }

    override fun readAltKey(): Boolean {
        val active = altActive
        altActive = false
        return active
    }

    override fun readShiftKey() = false

    override fun readFnKey() = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?) = false

    override fun onEmulatorSet() {}

    fun toggleKeyboard() {
        view.requestFocus()
        if (imm.isActive(view)) imm.hideSoftInputFromWindow(view.windowToken, 0)
        else imm.showSoftInput(view, 0)
    }

    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}
}

/** 终端会话持有者：主界面期间存活，进程退出后可一键重建 */
class RealTerminal(
    val shellPath: String,
    private val cwd: String,
    private val env: Array<String>,
    val client: OzjTerminalSessionClient,
    val viewClient: OzjTerminalViewClient,
    session: TerminalSession,
) {
    var session: TerminalSession = session
        private set

    private var bannerSent = false

    val isRunning: Boolean get() = session.isRunning()

    fun newSession(): TerminalSession = TerminalSession(shellPath, cwd, null, env, TRANSCRIPT_ROWS, client)

    /** 首次挂载时把工具链提示打进终端（作为 shell 输入执行，输出自然回显） */
    fun sendBannerIfNeeded(binPath: String) {
        if (bannerSent) return
        bannerSent = true
        val missing = com.android.ozjflash.flash.NativeLibs.missingTools(binPath)
        val hint = if (missing.isEmpty()) {
            "[OZJFLASH] tools ready, PATH includes: $binPath"
        } else {
            "[OZJFLASH] tools: $binPath (missing: ${missing.joinToString()})"
        }
        session.writeBytes("printf '\\\\n%s\\\\n' '$hint'\n".toByteArray(Charsets.UTF_8))
    }

    fun restart() {
        try {
            session.finishIfRunning()
        } catch (_: Throwable) {
        }
        session = newSession()
        bannerSent = false
        val v = client.view
        if (v != null) {
            v.attachSession(session)
            client.view = v
        }
    }

    fun sendInput(text: String) = session.writeBytes(text.toByteArray(Charsets.UTF_8))
    fun sendLine(line: String) = sendInput(line + "\n")
    fun sendCtrlC() = session.writeBytes(byteArrayOf(3))
    fun sendEsc() = session.writeBytes(byteArrayOf(27))
    fun sendTab() = session.writeBytes(byteArrayOf(9))
    fun sendArrow(seq: String) = session.writeBytes(seq.toByteArray(Charsets.UTF_8))

    companion object {
        /** 滚回缓冲行数（Termux 默认 2000） */
        internal const val TRANSCRIPT_ROWS = 2000

        /** 方向键转义序列 */
        const val ARROW_UP = "\u001b[A"
        const val ARROW_DOWN = "\u001b[B"
        const val ARROW_RIGHT = "\u001b[C"
        const val ARROW_LEFT = "\u001b[D"
    }
}

/** 在主界面组合期间持有唯一的真终端会话（PTY + bash） */
@Composable
fun rememberRealTerminal(shellPath: String, binPath: String, nativeDir: String?): RealTerminal {
    return remember(shellPath) {
        val client = OzjTerminalSessionClient()
        val viewClient = OzjTerminalViewClient()
        val env = com.android.ozjflash.flash.NativeLibs.shellEnv(ozjApp, binPath, nativeDir)
            .map { (k, v) -> "$k=$v" }
            .toTypedArray()
        val session = TerminalSession(
            shellPath,
            ozjApp.filesDir.absolutePath,
            null,
            env,
            RealTerminal.Companion.TRANSCRIPT_ROWS,
            client,
        )
        RealTerminal(shellPath, ozjApp.filesDir.absolutePath, env, client, viewClient, session)
    }
}

/** 把 Termux 的 TerminalView 挂进 Compose 树（黑底、ANSI 渲染、软键盘输入） */
@Composable
fun RealTerminalView(
    terminal: RealTerminal,
    binPath: String,
    modifier: Modifier = Modifier,
) {
    val viewClient = terminal.viewClient
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TerminalView(context, null).apply {
                setTerminalViewClient(viewClient)
                viewClient.view = this
                terminal.client.view = this
                setTextSize(13)
                isFocusable = true
                isFocusableInTouchMode = true
                attachSession(terminal.session)
                terminal.sendBannerIfNeeded(binPath)
                requestFocus()
            }
        },
    )
}
