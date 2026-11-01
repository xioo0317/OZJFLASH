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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix 主体的终端页 */
@Composable
fun TerminalMiuix(
    console: TerminalConsole,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val engine = console.engine
    val running by engine.running.collectAsStateWithLifecycle()
    val colorScheme = MiuixTheme.colorScheme
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
        SmallTitle(
            text = if (running) "终端已就绪" else "终端未运行",
            modifier = Modifier.padding(top = 6.dp)
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(10.dp)
            ) {
                Text(
                    text = console.output.ifEmpty { "尚无输出，输入命令后回车执行" },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            BasicTextField(
                value = console.input,
                onValueChange = console::onInput,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { console.submit() })
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rememberQuickActions(console).forEach { action ->
                TextButton(text = action.label, onClick = action.run)
            }
        }
    }
}
