package com.android.ozjflash.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.viewmodel.AppUiState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/** Miuix 主体的设置页 */
@Composable
fun SettingsMiuix(
    uiState: AppUiState,
    actions: SettingsActions,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val colorScheme = MiuixTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .then(
                if (bottomInnerPadding > 0.dp) Modifier.padding(bottom = bottomInnerPadding)
                else Modifier.windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Bottom)
                        .union(WindowInsets.navigationBars)
                )
            )
            .verticalScroll(rememberScrollStateCompat())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmallTitle(text = stringResource(R.string.settings_ui_mode), modifier = Modifier.padding(top = 8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                UiMode.entries.forEach { mode ->
                    TextButton(
                        text = if (mode == UiMode.Miuix) stringResource(R.string.settings_ui_mode_miuix)
                        else stringResource(R.string.settings_ui_mode_material),
                        onClick = { actions.setUiMode(mode) },
                        enabled = uiState.uiMode != mode
                    )
                }
            }
        }

        SmallTitle(text = stringResource(R.string.settings_theme))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                colorModeOptions.forEach { (mode, label) ->
                    Text(
                        text = stringResource(label),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .clickable { actions.setColorMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        color = if (uiState.appSettings.colorMode == mode) colorScheme.primary else colorScheme.onSurface
                    )
                }
            }
        }

        SmallTitle(text = stringResource(R.string.settings_color))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                keyColorOptions.forEach { (value, _) ->
                    val selected = uiState.appSettings.keyColor == value
                    Box(
                        modifier = Modifier
                            .size(if (selected) 40.dp else 32.dp)
                            .clip(CircleShape)
                            .background(if (value == 0) colorScheme.primary else Color(value))
                            .clickable { actions.setKeyColor(value) }
                    )
                }
            }
        }

        SmallTitle(text = stringResource(R.string.settings_page_scale))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pageScaleOptions.forEach { scale ->
                    TextButton(
                        text = "${scale}x",
                        onClick = { actions.setPageScale(scale) },
                        enabled = uiState.pageScale != scale
                    )
                }
            }
        }

        SmallTitle(text = stringResource(R.string.settings_others))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SwitchItem(
                    title = stringResource(R.string.settings_enable_blur),
                    summary = stringResource(R.string.settings_enable_blur_summary),
                    checked = uiState.enableBlur,
                    onCheckedChange = actions::setBlur
                )
                SwitchItem(
                    title = stringResource(R.string.settings_enable_floating_bottom_bar),
                    summary = stringResource(R.string.settings_enable_floating_bottom_bar_summary),
                    checked = uiState.enableFloatingBottomBar,
                    onCheckedChange = actions::setFloatingBottomBar
                )
                SwitchItem(
                    title = stringResource(R.string.settings_terminal_pty),
                    summary = stringResource(R.string.settings_terminal_pty_summary),
                    checked = uiState.terminalPty,
                    onCheckedChange = actions::setTerminalPty
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            ArrowPreference(
                title = stringResource(R.string.about),
                onClick = actions::openAbout
            )
        }
    }
}

@Composable
private fun SwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp)
            Text(
                text = summary,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary
            )
        }
        top.yukonga.miuix.kmp.basic.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun rememberScrollStateCompat() = androidx.compose.foundation.rememberScrollState()
