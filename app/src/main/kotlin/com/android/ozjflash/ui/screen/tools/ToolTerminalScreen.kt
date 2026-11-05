package com.android.ozjflash.ui.screen.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.FastbootConsole
import com.android.ozjflash.flash.FlashBridge
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.material.ExpressiveScaffold
import com.android.ozjflash.ui.component.material.expressiveTopAppBarColors
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 工具种类：仅保留标题资源，二级页按需渲染对应工具 */
enum class ToolKind(@StringRes val titleRes: Int) {
    FASTBOOT(R.string.tools_fastboot),
    ADB(R.string.tools_adb),
}

/**
 * 工具二级页入口：按当前 UI 主体分派到 Miuix / Material 两套外壳。
 *
 * - Fastboot：命令行执行页（输入框 + 执行 + 输出），命令以独立短命进程直接
 *   exec 内置 fastboot 二进制，不占用常驻 bash；
 * - ADB：暂为占位页，工具功能后续补齐。
 * 两套外壳的顶栏均随内容滚动而折叠。
 */
@Composable
fun ToolPageScreen(navigator: Navigator, tool: ToolKind) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ToolPageMiuix(navigator, tool)
        UiMode.Material -> ToolPageMaterial(navigator, tool)
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
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val layoutDirection = LocalLayoutDirection.current

    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    color = barColor,
                    title = title,
                    navigationIcon = {
                        MiuixIconButton(onClick = { navigator.pop() }) {
                            MiuixIcon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                            )
                        }
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
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
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
                item {
                    if (tool == ToolKind.FASTBOOT) {
                        FastbootConsoleMiuix()
                    } else {
                        EmptyToolPlaceholder()
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolPageMaterial(navigator: Navigator, tool: ToolKind) {
    val title = stringResource(tool.titleRes)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.systemBars
                    .add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            item {
                if (tool == ToolKind.FASTBOOT) {
                    FastbootConsoleMaterial()
                } else {
                    EmptyToolPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun EmptyToolPlaceholder() {
    // 注意：Miuix 组件依赖 MiuixTheme，Material 模式下必须用 Material Text，否则会崩
    val isMiuix = LocalUiMode.current == UiMode.Miuix
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isMiuix) {
            MiuixText(
                text = stringResource(R.string.empty_content),
                fontSize = 16.sp,
                color = Color.Gray,
            )
        } else {
            Text(
                text = stringResource(R.string.empty_content),
                fontSize = 16.sp,
                color = Color.Gray,
            )
        }
    }
}

/* ------------------------------ Fastboot 命令行 ------------------------------ */

/**
 * 命令行状态：输入 / 输出 / 执行中。
 * 每条命令由 FastbootConsole 以独立进程执行，执行完即退出，不占用终端会话。
 */
@Stable
private class FastbootConsoleState(
    private val context: android.content.Context,
    private val scope: CoroutineScope,
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
                FastbootConsole.run(context, cmd, FlashBridge.rootMode.value) { line ->
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
private fun rememberFastbootConsoleState(): FastbootConsoleState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        FastbootConsoleState(context.applicationContext, scope)
    }
}

@Composable
private fun FastbootConsoleMiuix() {
    val state = rememberFastbootConsoleState()
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
                    text = stringResource(R.string.tools_cmd_desc),
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
                                    text = stringResource(R.string.tools_cmd_hint),
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

        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MiuixText(
                    text = stringResource(R.string.tools_cmd_output),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            colorScheme.onSurface.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                ) {
                    SelectionContainer {
                        MiuixText(
                            text = state.output.ifEmpty { stringResource(R.string.tools_cmd_empty) },
                            fontSize = 12.sp,
                            color = if (state.output.isEmpty()) {
                                Color.Gray
                            } else {
                                colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FastbootConsoleMaterial() {
    val state = rememberFastbootConsoleState()
    val rootMode by FlashBridge.rootMode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.tools_cmd_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.tools_cmd_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.input,
                    onValueChange = { state.input = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.tools_cmd_hint)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { state.execute() }),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { state.execute() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.running,
                    ) {
                        Text(
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
                    Text(
                        text = "Root",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.tools_cmd_output),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = state.output.ifEmpty { stringResource(R.string.tools_cmd_empty) },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (state.output.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
