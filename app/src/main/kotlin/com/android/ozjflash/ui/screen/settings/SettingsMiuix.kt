package com.android.ozjflash.ui.screen.settings

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ozjflash.R
import com.android.ozjflash.findActivity
import com.android.ozjflash.flash.FlashBridge
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import com.android.ozjflash.ui.viewmodel.AppUiState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Miuix 主体的设置页（照搬 KernelSU 的卡片 + 偏好组件结构）。
 *
 * 与 KernelSU 一致：这里只放「界面主体 / 语言 / 主题设置入口 / 终端 / 诊断」，
 * 具体配色、模糊、悬浮底栏、缩放等全部收进二级页「主题设置」（Route.ColorPalette）。
 */
@Composable
fun SettingsMiuix(
    uiState: AppUiState,
    actions: SettingsActions,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior(
        rememberTopAppBarState(),
        { true },
        spring<Float>(),
        exponentialDecay<Float>(),
    )
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val context = LocalContext.current
    var showCrashLog by remember { mutableStateOf(false) }
    val rootMode by FlashBridge.rootMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings),
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
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // 外观：界面切换 / 主题设置 / 语言（二级页）
                        Card(modifier = Modifier.fillMaxWidth()) {
                            val uiModeItems = listOf(
                                stringResource(R.string.settings_ui_mode_miuix),
                                stringResource(R.string.settings_ui_mode_material),
                            )
                            OverlayDropdownPreference(
                                title = stringResource(R.string.settings_ui_mode),
                                summary = stringResource(R.string.settings_ui_mode_summary),
                                items = uiModeItems,
                                startAction = {
                                    Icon(
                                        Icons.Rounded.DisplaySettings,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.settings_ui_mode),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                selectedIndex = if (uiState.uiMode == UiMode.Miuix) 0 else 1,
                                onSelectedIndexChange = {
                                    actions.setUiMode(if (it == 0) UiMode.Miuix else UiMode.Material)
                                },
                            )

                            ArrowPreference(
                                title = stringResource(R.string.settings_theme),
                                summary = stringResource(R.string.settings_theme_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Palette,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.settings_theme),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions::openColorPalette,
                            )

                            val langEntries = listOf(
                                "system" to stringResource(R.string.language_system),
                                "en" to stringResource(R.string.language_en),
                                "zh-CN" to stringResource(R.string.language_zh_rCN),
                                "zh-TW" to stringResource(R.string.language_zh_rTW),
                                "ja" to stringResource(R.string.language_ja),
                                "es" to stringResource(R.string.language_es),
                            )
                            val langIndex = langEntries.indexOfFirst { it.first == uiState.language }.coerceAtLeast(0)
                            OverlayDropdownPreference(
                                title = stringResource(R.string.settings_language),
                                summary = stringResource(R.string.settings_language_summary),
                                items = langEntries.map { it.second },
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Language,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.settings_language),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                selectedIndex = langIndex,
                                onSelectedIndexChange = {
                                    actions.setLanguage(langEntries[it].first)
                                    context.findActivity()?.recreate()
                                },
                            )
                        }

                        // 刷写执行模式：false=免 root 走 USB 原生桥；true=Root 直接 su 执行工具
                        Card(modifier = Modifier.fillMaxWidth()) {
                            SwitchPreference(
                                title = stringResource(R.string.settings_root_mode),
                                summary = stringResource(R.string.settings_root_mode_summary),
                                checked = rootMode,
                                onCheckedChange = { actions.setRootMode(it) },
                                startAction = {
                                    Icon(
                                        Icons.Rounded.AdminPanelSettings,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.settings_root_mode),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                            )
                        }

                        // 诊断 / 其它
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ArrowPreference(
                                title = stringResource(R.string.terminal),
                                summary = stringResource(R.string.terminal_open_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Terminal,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.terminal),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions::openTerminal,
                            )
                            ArrowPreference(
                                title = stringResource(R.string.settings_log),
                                summary = stringResource(R.string.settings_log_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.BugReport,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.settings_log),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { showCrashLog = true },
                            )
                            ArrowPreference(
                                title = stringResource(R.string.about),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Info,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(R.string.about),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions::openAbout,
                            )
                        }

                        Spacer(Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }

    if (showCrashLog) {
        CrashLogDialog(onDismiss = { showCrashLog = false })
    }
}
