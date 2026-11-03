package com.android.ozjflash.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ozjflash.R
import com.android.ozjflash.findActivity
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.material.ExpressiveScaffold
import com.android.ozjflash.ui.component.material.SegmentedColumn
import com.android.ozjflash.ui.component.material.SegmentedDropdownItem
import com.android.ozjflash.ui.component.material.SegmentedListItem
import com.android.ozjflash.ui.component.material.SegmentedSwitchItem
import com.android.ozjflash.ui.component.material.expressiveTopAppBarColors
import com.android.ozjflash.ui.viewmodel.AppUiState

/**
 * Material3 Expressive 主体的设置页（照搬 KernelSU 的 SegmentedColumn 写法）。
 *
 * 分组与 Miuix 版完全一致：界面主体 / 语言 / 主题设置入口 → 终端 → 诊断。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMaterial(
    uiState: AppUiState,
    actions: SettingsActions,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    var showCrashLog by remember { mutableStateOf(false) }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            val uiModeItems = listOf(
                stringResource(R.string.settings_ui_mode_miuix),
                stringResource(R.string.settings_ui_mode_material),
            )
            val langEntries = listOf(
                "system" to stringResource(R.string.language_system),
                "en" to stringResource(R.string.language_en),
                "zh-CN" to stringResource(R.string.language_zh_rCN),
                "zh-TW" to stringResource(R.string.language_zh_rTW),
                "ja" to stringResource(R.string.language_ja),
                "es" to stringResource(R.string.language_es),
            )
            val themeEntry = stringResource(R.string.settings_theme)

            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedDropdownItem(
                            icon = Icons.Filled.DisplaySettings,
                            title = stringResource(R.string.settings_ui_mode),
                            summary = stringResource(R.string.settings_ui_mode_summary),
                            items = uiModeItems,
                            selectedIndex = if (uiState.uiMode == UiMode.Miuix) 0 else 1,
                            onItemSelected = {
                                actions.setUiMode(if (it == 0) UiMode.Miuix else UiMode.Material)
                            },
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = actions::openColorPalette,
                            headlineContent = { Text(themeEntry) },
                            supportingContent = { Text(stringResource(R.string.settings_theme_summary)) },
                            leadingContent = { Icon(Icons.Filled.Palette, themeEntry) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        )
                    },
                    {
                        SegmentedDropdownItem(
                            icon = Icons.Filled.Language,
                            title = stringResource(R.string.settings_language),
                            summary = stringResource(R.string.settings_language_summary),
                            items = langEntries.map { it.second },
                            selectedIndex = langEntries.indexOfFirst { it.first == uiState.language }.coerceAtLeast(0),
                            onItemSelected = {
                                actions.setLanguage(langEntries[it].first)
                                context.findActivity()?.recreate()
                            },
                        )
                    },
                ),
            )

            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = { showCrashLog = true },
                            headlineContent = { Text(stringResource(R.string.settings_log)) },
                            supportingContent = { Text(stringResource(R.string.settings_log_summary)) },
                            leadingContent = {
                                Icon(Icons.Filled.BugReport, stringResource(R.string.settings_log))
                            },
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = actions::openAbout,
                            headlineContent = { Text(stringResource(R.string.about)) },
                            leadingContent = { Icon(Icons.Filled.Info, stringResource(R.string.about)) },
                        )
                    },
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.height(bottomInnerPadding))
        }
    }

    if (showCrashLog) {
        CrashLogDialog(onDismiss = { showCrashLog = false })
    }
}
