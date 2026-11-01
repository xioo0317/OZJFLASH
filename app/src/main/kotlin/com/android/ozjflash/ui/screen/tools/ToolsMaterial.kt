package com.android.ozjflash.ui.screen.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.screen.terminal.TerminalConsole

/** Material3 Expressive 主体的工具箱页 */
@Composable
fun ToolsMaterial(
    state: ToolsState,
    console: TerminalConsole,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomInnerPadding)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = MaterialTheme.shapes.large,
            color = if (state.missingCount == 0) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(if (state.missingCount == 0) Icons.Rounded.Circle else Icons.Rounded.Circle, null)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (state.missingCount == 0) R.string.tools_ready else R.string.tools_missing),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${state.tools.size - state.missingCount}/${state.tools.size} ${stringResource(R.string.tools_present)}",
                        fontSize = 12.sp
                    )
                }
                androidx.compose.material3.FilledTonalButton(onClick = console::runCheckScript) {
                    Text(stringResource(R.string.tools_run_check))
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(state.tools.size) { index ->
                val tool = state.tools[index]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (tool.present) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    )
                    Text(tool.name, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text(
                        if (tool.present) "OK" else "缺失",
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            item { Box(Modifier.padding(bottom = 24.dp)) }
        }
    }
}
