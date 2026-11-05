package com.android.ozjflash.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
import java.io.File
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import kotlin.math.roundToInt

/** 终端字号（sp）：默认与 Termux 一致，双指缩放限制在同一区间 */
private const val DEFAULT_TEXT_SIZE = 13f
private const val MIN_TEXT_SIZE = 8f
private const val MAX_TEXT_SIZE = 32f

/** sp 转 px：Termux 的 TerminalView.setTextSize 接收的是像素值，需把 sp 字号换算后再传入，
 *  否则在 2x+ 密度屏上会被当成 px 渲染得过小 */
private fun Float.toPx(context: Context): Int =
    (this * context.resources.displayMetrics.scaledDensity).roundToInt()

/** Compose 层缩放后，内置手势在该时间窗内不再调整字号 */
private const val COMPOSE_ZOOM_WINDOW_MS = 400L

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

    private var textSizeSp = DEFAULT_TEXT_SIZE

    /** Compose 层 transformable 上次缩放的时刻：避免与 TerminalView 内置手势重复放大 */
    private var lastComposeZoomAt = 0L

    private val imm: InputMethodManager
        get() = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    /** 双指缩放：TerminalView 内部手势回调，累积比例超阈值后按步进调整字号 */
    override fun onScale(scale: Float): Float {
        if (SystemClock.uptimeMillis() - lastComposeZoomAt < COMPOSE_ZOOM_WINDOW_MS) {
            // Compose 层已经处理过这次捏合，内置手势只复位累积值，避免双倍缩放
            return 1f
        }
        if (scale < 0.9f || scale > 1.1f) {
            zoom(if (scale < 1f) 0.94f else 1.06f)
            return 1f
        }
        return scale
    }

    /** 由 Compose 层的 transformable 调用 */
    fun zoomFromCompose(factor: Float) {
        lastComposeZoomAt = SystemClock.uptimeMillis()
        zoom(factor)
    }

    /** 缩放字号（sp），供双指手势与顶栏/设置共用 */
    fun zoom(factor: Float) {
        textSizeSp = (textSizeSp * factor).coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
        if (::view.isInitialized) view.setTextSize(textSizeSp.toPx(view.context))
    }

    /** 重置为 Termux 默认字号 */
    fun resetZoom() {
        textSizeSp = DEFAULT_TEXT_SIZE
        if (::view.isInitialized) view.setTextSize(textSizeSp.toPx(view.context))
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

    val isRunning: Boolean get() = session.isRunning()

    fun newSession(): TerminalSession = TerminalSession(shellPath, cwd, null, env, TRANSCRIPT_ROWS, client)

    fun restart() {
        try {
            session.finishIfRunning()
        } catch (_: Throwable) {
        }
        session = newSession()
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

    /** 清屏：Ctrl+L，bash / zsh / mksh 都识别 */
    fun clearScreen() = session.writeBytes(byteArrayOf(12))

    /** 粘贴系统剪贴板到 PTY（顶栏按钮用） */
    fun pasteFromClipboard() {
        val ctx = ozjApp
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString() ?: return
        sendInput(text)
    }

    companion object {
        /** 滚回缓冲行数（Termux 默认 2000） */
        internal const val TRANSCRIPT_ROWS = 2000

        /** 方向键转义序列 */
        const val ARROW_UP = "\u001b[A"
        const val ARROW_DOWN = "\u001b[B"
        const val ARROW_RIGHT = "\u001b[C"
        const val ARROW_LEFT = "\u001b[D"

        /** Termux 默认 extra-keys 里的其余功能键序列 */
        const val KEY_HOME = "\u001b[H"
        const val KEY_END = "\u001b[F"
        const val KEY_PAGE_UP = "\u001b[5~"
        const val KEY_PAGE_DOWN = "\u001b[6~"
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
        // 写 .ozjflash.rc / .bashrc / .mkshrc（含 cd 进应用私有目录），副作用必须保留
        com.android.ozjflash.flash.NativeLibs.prepareShell(ozjApp, binPath)
        // 启动 cwd 必须整条路径对应用 UID 可遍历，否则 mksh 启动时 getcwd() 会在
        // /data/data(0700) 或 /data/local(0751) 这类祖先目录吃 EACCES，报
        //   shell-init: error retrieving current directory: getcwd: ... Permission denied
        // 用 /data（或回溯的 /）做启动 cwd；rc 末尾会 cd 进应用私有目录，提示符仍显示私有目录，且不打印报错。
        val cwd = if (File("/data").isDirectory) "/data" else "/"
        val session = TerminalSession(
            shellPath,
            cwd,
            null,
            env,
            RealTerminal.Companion.TRANSCRIPT_ROWS,
            client,
        )
        RealTerminal(shellPath, cwd, env, client, viewClient, session)
    }
}

/** 把 Termux 的 TerminalView 挂进 Compose 树（黑底、ANSI 渲染、软键盘输入、双指缩放） */
@Composable
fun RealTerminalView(
    terminal: RealTerminal,
    modifier: Modifier = Modifier,
) {
    val viewClient = terminal.viewClient
    // Compose 层的双指缩放兜底：TerminalView 自身的手势偶尔会被外层分页手势吃掉，
    // 这里再用 transformable 接一次（canPan=false，单指滚动仍交给 TerminalView）
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        viewClient.zoomFromCompose(zoomChange)
    }
    AndroidView(
        modifier = modifier.transformable(state = transformState, canPan = { false }),
        factory = { context ->
            TerminalView(context, null).apply {
                setTerminalViewClient(viewClient)
                viewClient.view = this
                terminal.client.view = this
                setTextSize(DEFAULT_TEXT_SIZE.toPx(context))
                isFocusable = true
                isFocusableInTouchMode = true
                attachSession(terminal.session)
                requestFocus()
            }
        },
    )
}
