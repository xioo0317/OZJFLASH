package com.android.ozjflash.ui.screen.tools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.FlashBridge
import com.android.ozjflash.flash.ToolConsole
import com.android.ozjflash.flash.UsbFdSession
import com.android.ozjflash.flash.UsbHost
import com.android.ozjflash.ui.component.ToolManualDialog
import com.android.ozjflash.ui.component.GlassBackButton
import com.android.ozjflash.ui.component.GlassHelpButton
import com.android.ozjflash.ui.component.TerminalOutputWindow
import com.android.ozjflash.ui.component.usb.UsbPillCollapseActions
import com.android.ozjflash.ui.component.usb.UsbStatusPill
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import com.android.ozjflash.ui.util.rememberRevampBackdrop
import com.android.ozjflash.ui.util.revampSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import com.android.ozjflash.ui.util.RevampTopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 工具种类：标题 + 对应的命令行二进制名 */
enum class ToolKind(@StringRes val titleRes: Int, val bin: String) {
    FASTBOOT(R.string.tools_fastboot, "fastboot"),
    ADB(R.string.tools_adb, "adb"),
}

/**
 * 工具二级页入口：按当前 UI 主体分派到 Miuix / Material 两套外壳。
 *
 * Fastboot 与 ADB 都是命令行执行页（输入框 + 执行 + 输出），顶栏右侧带
 * USB 状态胶囊（与工具页/终端同一组件）；命令以独立短命进程执行，
 * 不占用常驻 bash，免 root 时经 FdPass 注入已授权的 USB fd。
 */
@Composable
fun ToolPageScreen(navigator: Navigator, tool: ToolKind) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val rootMode by FlashBridge.rootMode.collectAsStateWithLifecycle()

    /**
     * 复刻 MOFLASH 的「打开页面就自动访问连接 USB」：
     * 页面组合期注册权限广播，授权回调 / 设备插入后立刻 ensure 建立常驻 usbfs fd；
     * 组合当下也立即试一次（已授权→直接 open，未授权→弹系统授权框）。
     *
     * 关键：页面销毁只注销广播，**不 release fd**。fd 常驻单例是免 root 下
     * 连续执行多条命令、以及 fdpass 服务与句柄生命周期不错位的前提。
     */
    DisposableEffect(tool.bin, rootMode) {
        var receiver: BroadcastReceiver? = null
        if (!rootMode) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    when (intent.action) {
                        UsbHost.ACTION_USB_PERMISSION -> {
                            if (!intent.getBooleanExtra(
                                    UsbManager.EXTRA_PERMISSION_GRANTED, false,
                                )
                            ) return
                        }
                        UsbManager.ACTION_USB_DEVICE_ATTACHED -> Unit
                        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                            // 设备拔出/reboot 后旧句柄失效，清空会话，下次 ensure 重新 open
                            UsbHost.refresh(ctx)
                            UsbFdSession.release(keepFd = false)
                            return
                        }
                        else -> return
                    }
                    UsbHost.refresh(ctx)
                    UsbFdSession.ensure(ctx, tool.bin)
                }
            }
            val filter = UsbHost.intentFilter()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter)
            }
            UsbHost.refresh(appContext)
            UsbFdSession.ensure(appContext, tool.bin)
        }
        onDispose {
            receiver?.let { runCatching { appContext.unregisterReceiver(it) } }
        }
    }

    ToolPageMiuix(navigator, tool)
}

/**
 * 工具二级页顶栏右侧动作。
 *
 * 不再按「有没有设备」切换胶囊与 ? 号：USB 胶囊在 ADB / Fastboot 页都常驻，
 * 且 ADB 与 Fastboot 页都保留一个 [GlassHelpButton] 说明书入口
 * （按当前 [tool] 弹出 ADB / Fastboot 各自的说明文案）。
 *
 * Miuix 页传入 [scrollBehavior] 做折叠联动，交给 [UsbPillCollapseActions] 按 Miuix
 * 大小标题切换机制处理：展开态胶囊下移对齐大标题并随其渐出、? 号留在右上原位；
 * 折叠态胶囊渐入归位、? 号隐藏让位。两者在同一位置交替，不会互相挤位。
 * 折叠联动同时把静止态 showShadow 传给 ? 号按钮：滑动淡化时 ? 号与 USB 胶囊
 * 一样取消 dropShadow 阴影，避免半透明合成层露出矩形阴影块。
 * M3 页不传，胶囊与 ? 号都常驻（并排）、阴影始终绘制。
 */
@Composable
private fun ToolPageActions(
    tool: ToolKind,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior? = null,
) {
    var manualTool by remember { mutableStateOf<ToolKind?>(null) }
    val help: @Composable (Modifier, Boolean) -> Unit = { mod, showShadow ->
        GlassHelpButton(
            onClick = { manualTool = tool },
            modifier = mod,
            showShadow = showShadow,
        )
    }

    if (scrollBehavior != null) {
        UsbPillCollapseActions(scrollBehavior = scrollBehavior, help = help)
    } else {
        help(Modifier, true)
        UsbStatusPill()
    }

    manualTool?.let { kind ->
        ToolManualDialog(
            fastboot = kind == ToolKind.FASTBOOT,
            onDismiss = { manualTool = null },
        )
    }
}

@Composable
private fun ToolPageMiuix(navigator: Navigator, tool: ToolKind) {
    val title = stringResource(tool.titleRes)
    val scrollBehavior = MiuixScrollBehavior(
        rememberMiuixTopAppBarState(),
        { true },
        androidx.compose.animation.core.spring<Float>(),
        androidx.compose.animation.core.exponentialDecay<Float>(),
    )
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val revampBackdrop = rememberRevampBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop, couiBackdrop = revampBackdrop) {
                MiuixTopAppBar(
                    color = barColor,
                    title = title,
                    navigationIcon = {
                        GlassBackButton(
                            imageVector = MiuixIcons.Back,
                            onClick = { navigator.pop() },
                            mirrorForRtl = true,
                        )
                    },
                    actions = {
                        ToolPageActions(tool = tool, scrollBehavior = scrollBehavior)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = Modifier
            .revampSource(revampBackdrop)
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item { ToolConsoleMiuix(tool.bin) }
                item {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars
                                .asPaddingValues()
                                .calculateBottomPadding() + 12.dp,
                        ),
                    )
                }
            }
        }
    }
}

/* ------------------------------ 命令行 ------------------------------ */

/**
 * 命令行状态：输入 / 输出 / 执行中。
 * 每条命令由 ToolConsole 以独立进程执行，执行完即退出，不占用终端会话。
 */
@Stable
private class ToolConsoleState(
    private val context: android.content.Context,
    private val scope: CoroutineScope,
    val bin: String,
) {
    var input by mutableStateOf("")
    var output by mutableStateOf("")
    var running by mutableStateOf(false)

    fun execute() {
        val cmd = input.trim()
        if (cmd.isEmpty() || running) return
        running = true
        output = ""
        scope.launch {
            val sb = StringBuilder()
            val code = withContext(Dispatchers.IO) {
                ToolConsole.run(context, cmd, bin, FlashBridge.rootMode.value) { line ->
                    sb.appendLine(line)
                    // 逐行刷新（回调在 IO 线程，切主线程写 Compose 状态）
                    scope.launch(Dispatchers.Main.immediate) { output = sb.toString() }
                }
            }
            output = buildString {
                append(sb)
                if (isNotEmpty() && !endsWith("\n")) append('\n')
                append(if (code == 0) "✔ exit=0" else "✘ exit=$code")
            }
            running = false
        }
    }
}

@Composable
private fun rememberToolConsoleState(bin: String): ToolConsoleState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope, bin) {
        ToolConsoleState(context.applicationContext, scope, bin)
    }
}

@StringRes
private fun descRes(bin: String): Int =
    if (bin == "adb") R.string.tools_adb_cmd_desc else R.string.tools_cmd_desc

@StringRes
private fun hintRes(bin: String): Int =
    if (bin == "adb") R.string.tools_adb_cmd_hint else R.string.tools_cmd_hint

@Composable
private fun ToolConsoleMiuix(bin: String) {
    val state = rememberToolConsoleState(bin)
    val rootMode by FlashBridge.rootMode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MiuixText(
                    text = stringResource(R.string.tools_cmd_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                MiuixText(
                    text = stringResource(descRes(bin)),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    value = state.input,
                    onValueChange = { state.input = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { state.execute() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    colorScheme.onSurface.copy(alpha = 0.08f),
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            if (state.input.isEmpty()) {
                                MiuixText(
                                    text = stringResource(hintRes(bin)),
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixButton(
                        onClick = { state.execute() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.running,
                    ) {
                        MiuixText(
                            text = if (state.running) {
                                stringResource(R.string.tools_cmd_running)
                            } else {
                                stringResource(R.string.tools_cmd_run)
                            },
                        )
                    }
                }
                if (rootMode) {
                    Spacer(Modifier.height(8.dp))
                    MiuixText(
                        text = "Root",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        TerminalOutputWindow(
            output = state.output,
            emptyText = stringResource(R.string.tools_cmd_empty),
        )
    }
}
