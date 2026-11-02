package com.android.ozjflash.ui.screen.terminal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ozjflash.terminal.RealTerminal
import com.android.ozjflash.terminal.RealTerminalView

/** Material 主体的真终端页：与 Miuix 版共用同一个 PTY 会话与 TerminalView */
@Composable
fun TerminalMaterial(
    terminal: RealTerminal,
    binPath: String,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 12.dp)
            .then(
                if (bottomInnerPadding > 0.dp) Modifier.padding(bottom = bottomInnerPadding)
                else Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (terminal.isRunning) "终端已就绪" else "会话已结束",
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            RealTerminalView(terminal = terminal, binPath = binPath, modifier = Modifier.fillMaxSize())
        }

        TerminalExtraKeysRow(
            terminal = terminal,
            keyBackground = colorScheme.surfaceContainerHighest,
            keyContent = colorScheme.onSurface,
            activeBackground = colorScheme.primary,
            activeContent = colorScheme.onPrimary,
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rememberTerminalCommands(terminal, binPath).forEach { cmd ->
                FilledTonalButton(onClick = cmd.run) {
                    Text(cmd.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
