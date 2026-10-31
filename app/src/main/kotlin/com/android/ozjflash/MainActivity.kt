package com.android.ozjflash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.ozjflash.flash.FlashScreen
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.terminal.TerminalEngine
import com.android.ozjflash.terminal.TerminalScreen
import com.android.ozjflash.ui.theme.ComposeEmptyActivityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 释放内置刷写工具（adb/fastboot/bash...）到 files/bin 并加载 JNI 原生库
        val binPath = NativeLibs.prepare(this)
        NativeLibs.loadJni()

        setContent {
            ComposeEmptyActivityTheme {
                AppRoot(binPath)
            }
        }
    }
}

@Composable
fun AppRoot(binPath: String) {
    var tab by remember { mutableStateOf(0) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val shell = remember(binPath) { NativeLibs.shellPath(binPath) }
    val engine = remember(binPath) { TerminalEngine(scope, shell, binPath) }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("终端") }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("刷写") }
            }
        }
    ) { innerPadding ->
        when (tab) {
            0 -> TerminalScreen(engine)
            1 -> FlashScreen(engine)
        }
    }
}
