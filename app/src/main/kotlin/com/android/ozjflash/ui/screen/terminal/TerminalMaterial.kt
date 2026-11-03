package com.android.ozjflash.ui.screen.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.android.ozjflash.terminal.RealTerminal

/** Material 主体的真终端页：与 Miuix 版共用同一个 PTY 会话与外壳（全屏纯黑 + 上游键位） */
@Composable
fun TerminalMaterial(
    terminal: RealTerminal,
    binPath: String,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    TerminalPane(terminal, binPath, bottomInnerPadding)
}
