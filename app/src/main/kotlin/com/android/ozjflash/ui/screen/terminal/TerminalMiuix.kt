package com.android.ozjflash.ui.screen.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.terminal.RealTerminal
import com.android.ozjflash.terminal.RealTerminalView
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val TERMINAL_CORNER = 14.dp

/** Miuix 主体的真终端页：Termux 上游 TerminalView（PTY + ANSI） */
@Composable
fun TerminalMiuix(
    terminal: RealTerminal,
    binPath: String,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val colorScheme = MiuixTheme.colorScheme

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
        SmallTitle(
            text = if (terminal.isRunning) "终端已就绪" else "会话已结束",
            modifier = Modifier.padding(top = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(TERMINAL_CORNER))
        ) {
            RealTerminalView(terminal = terminal, binPath = binPath, modifier = Modifier.fillMaxSize())
        }

        TerminalExtraKeysRow(
            terminal = terminal,
            keyBackground = colorScheme.surfaceVariant,
            keyContent = colorScheme.onSurface,
            activeBackground = colorScheme.primary,
            activeContent = colorScheme.onPrimary,
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rememberTerminalCommands(terminal, binPath).forEach { cmd ->
                TextButton(text = cmd.label, onClick = cmd.run)
            }
        }
    }
}

/** Termux 风格快捷键行：ESC / CTRL / ALT / TAB / 方向键 / 键盘 / 重启 */
@Composable
internal fun TerminalExtraKeysRow(
    terminal: RealTerminal,
    keyBackground: Color,
    keyContent: Color,
    activeBackground: Color,
    activeContent: Color,
) {
    val client = terminal.viewClient
    val ctrlActive = client.ctrlActive
    val altActive = client.altActive

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TerminalKey(
            text = "ESC",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = terminal::sendEsc,
        )
        TerminalKey(
            text = "CTRL",
            modifier = Modifier.weight(1f),
            background = if (ctrlActive) activeBackground else keyBackground,
            content = if (ctrlActive) activeContent else keyContent,
            onClick = client::toggleCtrl,
        )
        TerminalKey(
            text = "ALT",
            modifier = Modifier.weight(1f),
            background = if (altActive) activeBackground else keyBackground,
            content = if (altActive) activeContent else keyContent,
            onClick = client::toggleAlt,
        )
        TerminalKey(
            text = "TAB",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = terminal::sendTab,
        )
        TerminalKey(
            text = "↑",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = { terminal.sendArrow(RealTerminal.ARROW_UP) },
        )
        TerminalKey(
            text = "↓",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = { terminal.sendArrow(RealTerminal.ARROW_DOWN) },
        )
        TerminalKey(
            text = "←",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = { terminal.sendArrow(RealTerminal.ARROW_LEFT) },
        )
        TerminalKey(
            text = "→",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = { terminal.sendArrow(RealTerminal.ARROW_RIGHT) },
        )
        TerminalKey(
            text = "⌨",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = client::toggleKeyboard,
        )
        TerminalKey(
            text = "↻",
            modifier = Modifier.weight(1f),
            background = keyBackground,
            content = keyContent,
            onClick = terminal::restart,
        )
    }
}

@Composable
private fun TerminalKey(
    text: String,
    modifier: Modifier,
    background: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(height = 38.dp, width = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}
