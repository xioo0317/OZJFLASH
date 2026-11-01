package com.android.ozjflash.ui.screen.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.android.ozjflash.R
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.screen.terminal.TerminalConsole

/** 单个内置工具的存在状态 */
data class ToolEntry(
    val name: String,
    val present: Boolean,
)

data class ToolsState(
    val tools: List<ToolEntry> = emptyList(),
    val missingCount: Int = 0,
)

@Composable
fun rememberToolsState(binDir: String): ToolsState {
    return remember(binDir) {
        val missing = NativeLibs.missingTools(binDir)
        val entries = NativeLibs.TOOLS.map { ToolEntry(it, it !in missing) }
        derivedStateOf { ToolsState(entries, missing.size) }
    }.value
}

internal fun toolIcon(present: Boolean): ImageVector =
    if (present) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline

internal val toolMissingLabel: @Composable () -> String = { stringResource(R.string.tools_missing) }
internal val toolReadyLabel: @Composable () -> String = { stringResource(R.string.tools_ready) }

@Composable
fun ToolsPager(
    console: TerminalConsole,
    binDir: String,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val state = rememberToolsState(binDir)
    when (LocalUiMode.current) {
        UiMode.Miuix -> ToolsMiuix(state, console, bottomInnerPadding, isCurrentPage)
        UiMode.Material -> ToolsMaterial(state, console, bottomInnerPadding, isCurrentPage)
    }
}
