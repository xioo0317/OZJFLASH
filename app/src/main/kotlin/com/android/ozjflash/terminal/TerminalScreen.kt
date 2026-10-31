package com.android.ozjflash.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardActions
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(engine: TerminalEngine) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(engine) {
        engine.output.collect { chunk -> output = output + chunk }
    }
    LaunchedEffect(Unit) { if (!engine.running.value) engine.start() }

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Surface(
            Modifier.weight(1f).fillMaxWidth(),
            color = Color(0xFF0B0F14)
        ) {
            LazyColumn(Modifier.fillMaxSize().padding(6.dp), listState) {
                item {
                    Text(
                        output,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF9BE9A8)
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", color = Color(0xFF4DD0E1), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    engine.writeLine(input); input = ""
                })
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "clear" to "clear",
                "reboot bootloader" to "reboot bootloader",
                "fastboot devices" to "fastboot devices",
                "getvar all" to "fastboot getvar all",
                "Ctrl+C" to "__CTRLC__"
            ).forEach { (label, cmd) ->
                Button(onClick = {
                    when (cmd) {
                        "clear" -> output = ""
                        "__CTRLC__" -> engine.sendCtrlC()
                        else -> scope.launch { engine.writeLine(cmd) }
                    }
                }) { Text(label, fontSize = 10.sp) }
            }
        }
    }
}
