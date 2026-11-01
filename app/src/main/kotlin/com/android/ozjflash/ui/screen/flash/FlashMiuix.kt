package com.android.ozjflash.ui.screen.flash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.miuix.EditText
import com.android.ozjflash.ui.screen.terminal.TerminalConsole
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix 主体的刷写页 */
@Composable
fun FlashMiuix(
    console: TerminalConsole,
    state: FlashState,
    running: Boolean,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val colorScheme = MiuixTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (bottomInnerPadding > 0.dp) Modifier.padding(bottom = bottomInnerPadding)
                else Modifier.windowInsetsPadding(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Bottom)
                        .union(WindowInsets.navigationBars)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmallTitle(
            text = stringResource(R.string.flash_hint),
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                EditText(
                    title = stringResource(R.string.flash_partition_label),
                    value = state.partition,
                    onValueChange = state::onPartition,
                    modifier = Modifier.fillMaxWidth()
                )
                EditText(
                    title = stringResource(R.string.flash_image_label),
                    value = state.image,
                    onValueChange = state::onImage,
                    summary = stringResource(R.string.flash_image_hint),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                flashActions(state).forEach { action ->
                    TextButton(
                        text = action.label,
                        onClick = { console.submit(action.command) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonPrimaryColors()
                    )
                }
            }
        }

        Text(
            text = if (running) stringResource(R.string.flash_terminal_ready)
            else stringResource(R.string.flash_terminal_stopped),
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariantSummary
        )
    }
}
