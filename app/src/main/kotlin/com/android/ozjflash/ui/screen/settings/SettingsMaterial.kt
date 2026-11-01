package com.android.ozjflash.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.theme.ColorMode
import com.android.ozjflash.ui.viewmodel.AppUiState

/** Material3 Expressive 主体的设置页 */
@Composable
fun SettingsMaterial(
    uiState: AppUiState,
    actions: SettingsActions,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = bottomInnerPadding + 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel(stringResource(R.string.settings_ui_mode))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UiMode.entries.forEach { mode ->
                TextButton(
                    onClick = { actions.setUiMode(mode) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (uiState.uiMode == mode) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Text(
                        if (mode == UiMode.Miuix) stringResource(R.string.settings_ui_mode_miuix)
                        else stringResource(R.string.settings_ui_mode_material),
                        fontSize = 12.sp
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.settings_theme))
        Column {
            colorModeOptions.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (mode, label) ->
                        TextButton(
                            onClick = { actions.setColorMode(mode) },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (uiState.appSettings.colorMode == mode)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Text(stringResource(label), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (uiState.uiMode == UiMode.Miuix) {
            SwitchRow(
                title = stringResource(R.string.settings_miuix_monet),
                summary = stringResource(R.string.settings_miuix_monet_summary),
                checked = uiState.appSettings.colorMode.isMonet,
                onCheckedChange = actions::setMiuixMonet
            )
        }

        SectionLabel(stringResource(R.string.settings_color))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            keyColorOptions.forEach { (value, _) ->
                val selected = uiState.appSettings.keyColor == value
                Box(
                    modifier = Modifier
                        .size(if (selected) 40.dp else 34.dp)
                        .clip(CircleShape)
                        .background(
                            if (value == 0) MaterialTheme.colorScheme.primary
                            else Color(value)
                        )
                        .clickable { actions.setKeyColor(value) }
                )
            }
        }

        SectionLabel(stringResource(R.string.settings_page_scale))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            pageScaleOptions.forEach { scale ->
                TextButton(
                    onClick = { actions.setPageScale(scale) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (uiState.pageScale == scale) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text("${scale}x", fontSize = 11.sp)
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 6.dp))

        SwitchRow(
            title = stringResource(R.string.settings_enable_blur),
            summary = stringResource(R.string.settings_enable_blur_summary),
            checked = uiState.enableBlur,
            onCheckedChange = actions::setBlur
        )
        SwitchRow(
            title = stringResource(R.string.settings_enable_floating_bottom_bar),
            summary = stringResource(R.string.settings_enable_floating_bottom_bar_summary),
            checked = uiState.enableFloatingBottomBar,
            onCheckedChange = actions::setFloatingBottomBar
        )
        if (uiState.enableFloatingBottomBar) {
            SwitchRow(
                title = stringResource(R.string.settings_enable_floating_bottom_bar_blur),
                summary = stringResource(R.string.settings_enable_floating_bottom_bar_blur_summary),
                checked = uiState.enableFloatingBottomBarBlur,
                onCheckedChange = actions::setFloatingBottomBarBlur
            )
        }
        SwitchRow(
            title = stringResource(R.string.settings_terminal_pty),
            summary = stringResource(R.string.settings_terminal_pty_summary),
            checked = uiState.terminalPty,
            onCheckedChange = actions::setTerminalPty
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.about), fontSize = 14.sp) },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { actions.openAbout() }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontSize = 14.sp) },
        supportingContent = { Text(summary, fontSize = 11.sp) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun RowScopeSpacer() = Box(Modifier.size(4.dp))

internal typealias RowScope = androidx.compose.foundation.layout.RowScope

@Composable
internal fun AlignEnd(content: @Composable RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
