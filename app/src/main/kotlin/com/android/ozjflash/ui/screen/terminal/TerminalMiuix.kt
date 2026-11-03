package com.android.ozjflash.ui.screen.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.terminal.RealTerminal
import com.android.ozjflash.terminal.RealTerminalView
import top.yukonga.miuix.kmp.basic.Text

/** 终端页背景：与 Termux 上游一致，恒为纯黑，不跟随主题 */
internal val TerminalBackground = Color(0xFF000000)

/** 顶栏 / 键帽配色：贴 Termux 深色主题 */
private val TerminalForeground = Color(0xFFE6E6E6)
private val TerminalKeyBackground = Color(0xFF1F1F1F)
private val TerminalKeyActive = Color(0xFF2F6B45)

/** Miuix 主体的真终端页：全屏纯黑，照 Termux 上游布局（顶栏 + 终端视图 + extra-keys） */
@Composable
fun TerminalMiuix(
    terminal: RealTerminal,
    binPath: String,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    TerminalPane(terminal, binPath, bottomInnerPadding)
}

/**
 * 两套 UI 主体共用的终端外壳。
 *
 * 布局对齐 Termux 上游：顶部一条会话栏（粘贴 / 清屏 / 重开会话），
 * 中间吃满的终端视图，底部两行 extra-keys（Termux 默认键位）。
 */
@Composable
internal fun TerminalPane(
    terminal: RealTerminal,
    binPath: String,
    bottomInnerPadding: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .imePadding()
    ) {
        TerminalTopBar(terminal)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(TerminalBackground)
                .then(
                    if (bottomInnerPadding <= 0.dp) {
                        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    } else Modifier
                )
        ) {
            RealTerminalView(terminal = terminal, binPath = binPath, modifier = Modifier.fillMaxSize())
        }

        TerminalExtraKeysRow(terminal)

        // 悬浮底栏悬浮在内容之上，用等同高度的黑色占位把最后一行键帽顶上来
        if (bottomInnerPadding > 0.dp) {
            Spacer(Modifier.height(bottomInnerPadding))
        }
    }
}

/** 会话顶栏：标题 + 粘贴 / 清屏 / 重开 */
@Composable
private fun TerminalTopBar(terminal: RealTerminal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.terminal),
            color = TerminalForeground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        TerminalTopBarAction(
            icon = Icons.Rounded.ContentPaste,
            contentDescription = stringResource(R.string.paste_text),
            onClick = terminal::pasteFromClipboard,
        )
        TerminalTopBarAction(
            icon = Icons.Rounded.DeleteSweep,
            contentDescription = stringResource(R.string.terminal_clear),
            onClick = terminal::clearScreen,
        )
        TerminalTopBarAction(
            icon = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.terminal_restart),
            onClick = terminal::restart,
        )
    }
}

@Composable
private fun TerminalTopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription, tint = TerminalForeground)
    }
}

/**
 * Termux 上游默认 extra-keys 布局（termux.properties 里的默认值）：
 *
 * ```
 * [['ESC','/','-','HOME','UP','END','PGUP'],
 *  ['TAB','CTRL','ALT','LEFT','DOWN','RIGHT','PGDN']]
 * ```
 */
@Composable
internal fun TerminalExtraKeysRow(terminal: RealTerminal) {
    val client = terminal.viewClient
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TerminalKey("ESC", onClick = terminal::sendEsc)
            TerminalKey("/", onClick = { terminal.sendInput("/") })
            TerminalKey("-", onClick = { terminal.sendInput("-") })
            TerminalKey("HOME", onClick = { terminal.sendArrow(RealTerminal.KEY_HOME) })
            TerminalKey("↑", onClick = { terminal.sendArrow(RealTerminal.ARROW_UP) })
            TerminalKey("END", onClick = { terminal.sendArrow(RealTerminal.KEY_END) })
            TerminalKey("PGUP", onClick = { terminal.sendArrow(RealTerminal.KEY_PAGE_UP) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TerminalKey("TAB", onClick = terminal::sendTab)
            TerminalKey(
                label = "CTRL",
                active = client.ctrlActive,
                onClick = client::toggleCtrl,
            )
            TerminalKey(
                label = "ALT",
                active = client.altActive,
                onClick = client::toggleAlt,
            )
            TerminalKey("←", onClick = { terminal.sendArrow(RealTerminal.ARROW_LEFT) })
            TerminalKey("↓", onClick = { terminal.sendArrow(RealTerminal.ARROW_DOWN) })
            TerminalKey("→", onClick = { terminal.sendArrow(RealTerminal.ARROW_RIGHT) })
            TerminalKey("PGDN", onClick = { terminal.sendArrow(RealTerminal.KEY_PAGE_DOWN) })
        }
    }
}

@Composable
private fun RowScope.TerminalKey(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) TerminalKeyActive else TerminalKeyBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TerminalForeground,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
