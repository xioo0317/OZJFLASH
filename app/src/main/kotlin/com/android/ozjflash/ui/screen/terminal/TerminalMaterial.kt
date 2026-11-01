package com.android.ozjflash.ui.screen.terminal

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Material3 Expressive 主体的终端页 */
@Composable
fun TerminalMaterial(
    console: TerminalConsole,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val engine = console.engine
    val running by engine.running.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    LaunchedEffect(engine) { if (!engine.running.value) engine.start() }
    LaunchedEffect(console.output) { scroll.animateScrollTo(scroll.maxValue) }

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
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (running) "● shell 运行中" else "○ shell 已退出",
                style = MaterialTheme.typography.labelLarge,
                color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            IconButton(onClick = { engine.start() }) {
                Icon(
                    if (running) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
            }
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Box(modifier = Modifier.verticalScroll(scroll).padding(10.dp)) {
                Text(
                    text = console.output.ifEmpty { "（尚无输出）" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        OutlinedTextField(
            value = console.input,
            onValueChange = console::onInput,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("输入命令后回车执行（如 fastboot devices）", fontSize = 12.sp) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { console.submit() }),
            leadingIcon = { Text("$ ", fontFamily = FontFamily.Monospace) }
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rememberQuickActions(console).forEach { action ->
                FilledTonalButton(onClick = action.run) {
                    Text(action.label, fontSize = 11.sp)
                }
            }
        }
    }
}

/** 保留一个内部共享的颜色常量，避免两套主体使用不同黑底 */
internal val TerminalSurfaceDark = androidx.compose.ui.graphics.Color(0xFF0B0F14)
internal val TerminalTextGreen = androidx.compose.ui.graphics.Color(0xFF9BE9A8)

@Composable
internal fun ClassicTerminalOutput(console: TerminalConsole, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(console.output) { scroll.animateScrollTo(scroll.maxValue) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurfaceDark)
            .verticalScroll(scroll)
            .padding(8.dp)
    ) {
        BasicTextField(
            value = console.output,
            onValueChange = { },
            readOnly = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TerminalTextGreen)
        )
    }
}
