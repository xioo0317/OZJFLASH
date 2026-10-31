package com.android.ozjflash.flash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.terminal.TerminalEngine
import kotlinx.coroutines.launch

@Composable
fun FlashScreen(engine: TerminalEngine) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("刷写快捷指令（通过内置终端执行已内置的 fastboot / adb 工具）", fontSize = 13.sp)
        val actions = listOf(
            "重启到 Bootloader" to FlashActions.rebootBootloader(),
            "fastboot devices" to FlashActions.fastbootDevices(),
            "getvar all" to FlashActions.getvarAll(),
            "fastboot reboot-bootloader" to FlashActions.rebootFastboot(),
            "adb devices" to FlashActions.adbDevices()
        )
        actions.forEach { (label, cmd) ->
            Button(onClick = { scope.launch { engine.writeLine(cmd) } }, Modifier.fillMaxWidth()) {
                Text(label, fontSize = 13.sp)
            }
        }
        Text(
            "说明：设备需进入 fastboot / EDL 模式并用 USB 连接；终端 PATH 已注入内置 " +
                "adb / fastboot / bash 等工具。底层协议库（libnativecmd / libqdl / libmtkcmd / " +
                "libmiunlock 等）亦随包提供，可通过对应 JNI 桥接类调用。",
            fontSize = 11.sp
        )
    }
}
