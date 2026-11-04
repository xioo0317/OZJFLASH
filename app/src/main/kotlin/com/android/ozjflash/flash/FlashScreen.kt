package com.android.ozjflash.flash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.terminal.TerminalEngine
import kotlinx.coroutines.launch

/**
 * 刷写中枢：把 fastboot / EDL / MTK / OTA payload 等流程收口到一屏。
 *
 * 执行策略（与 FlashBridge 一致）：
 *  - 默认免 root（usbhost）：优先用原生桥在进程内直接驱动设备（需 UsbHostManager 已打开并取得 fd）；
 *  - 若原生桥不可用或开启 root 模式，则把命令发到内置终端引擎，由 PATH 中的二进制执行。
 *
 * 本屏自带一个 TerminalEngine（与工具页同一套内置二进制），命令输出实时回显在底部日志框。
 */
@Composable
fun FlashScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rootMode by FlashBridge.rootMode.collectAsStateWithLifecycle()
    val devices by UsbHost.devices.collectAsStateWithLifecycle()
    var nativeStatus by remember { mutableStateOf<String?>(null) }

    val binPath = remember { NativeLibs.prepare(context) }
    val nativeDir = context.applicationInfo.nativeLibraryDir
    val shellPath = remember(binPath) { NativeLibs.shellPath(binPath) }
    val engine = remember {
        TerminalEngine(scope, shellPath, NativeLibs.pathEnv(binPath, nativeDir))
    }
    val log = remember { mutableStateListOf<String>() }

    DisposableEffect(engine) {
        onDispose { engine.destroy() }
    }
    DisposableEffect(engine) {
        val job = scope.launch {
            engine.output.collect { chunk -> log.add(chunk) }
        }
        engine.start()
        onDispose { job.cancel() }
    }

    val target = remember(devices) {
        val preferred = UsbHost.preferredKey(context)
        devices.firstOrNull { it.key == preferred }
            ?: devices.firstOrNull { it.granted && it.mode.isFlashable }
            ?: devices.firstOrNull()
    }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 模式开关
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("执行模式", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (rootMode) "Root：直接调用内置二进制（需环境具备访问能力）"
                            else "免 root：经 USB Host 原生桥直连设备",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = rootMode, onCheckedChange = { FlashBridge.setRootMode(it) })
                }
            }

            // 目标设备
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("目标设备", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (target == null) {
                        Text("未检测到 USB 设备（请连接并授权）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(target.label, fontSize = 13.sp)
                        Text("模式：${target.mode.label}  ·  ${if (target.granted) "已授权" else "未授权"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    nativeStatus?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // —— Fastboot ——
            // 执行策略（对标 MOFLASH 的 root/usbhost 双模式）：
            //  - 免 root（默认）：所有 fastboot 操作都走进程内原生桥（NativeCmd + USB fd），
            //    因为非 root 进程打不开 /dev/bus/usb，裸 fastboot 二进制必然识别不到设备；
            //  - root：直接用 su 以绝对路径执行内置 fastboot 二进制（root 可访问 /dev/bus/usb）。
            FlashCard(title = "Fastboot") {
                var partition by remember { mutableStateOf("") }
                var image by remember { mutableStateOf("") }
                OutlinedTextField(partition, { partition = it }, Modifier.fillMaxWidth(), label = { Text("分区名，如 boot / system / vendor") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(image, { image = it }, Modifier.fillMaxWidth(), label = { Text("镜像路径，如 /sdcard/boot.img") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (partition.isBlank() || image.isBlank()) return@Button
                        scope.launch {
                            nativeStatus = null
                            if (!rootMode) {
                                val ok = FlashBridge.flashViaNative(context, partition, image)
                                nativeStatus = if (ok) "原生桥已提交刷写：$partition <- $image"
                                else "原生桥不可用，请在「设置」开启 Root 模式用 su 执行"
                            } else {
                                engine.writeLine(FlashBridge.cmdSuFastboot(binPath, "flash $partition $image"))
                                nativeStatus = "已提交（Root 模式：su 执行 fastboot）"
                            }
                        }
                    }, Modifier.weight(1f)) { Text("刷写分区") }
                    Button(onClick = {
                        scope.launch {
                            nativeStatus = null
                            if (!rootMode) {
                                val ok = FlashBridge.getvarViaNative(context)
                                nativeStatus = if (ok) "原生桥已提交 getvar all"
                                else "原生桥不可用，请在「设置」开启 Root 模式用 su 执行"
                            } else {
                                engine.writeLine(FlashBridge.cmdSuFastboot(binPath, "getvar all"))
                                nativeStatus = "已提交（Root 模式：su 执行）"
                            }
                        }
                    }, Modifier.weight(1f)) { Text("getvar all") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            nativeStatus = null
                            if (!rootMode) {
                                val ok = FlashBridge.rebootViaNative(context, "bootloader")
                                nativeStatus = if (ok) "原生桥已提交：重启到 Bootloader"
                                else "原生桥不可用，请在「设置」开启 Root 模式用 su 执行"
                            } else {
                                engine.writeLine(FlashBridge.cmdSuFastboot(binPath, "reboot bootloader"))
                                nativeStatus = "已提交（Root 模式：su 执行）"
                            }
                        }
                    }, Modifier.weight(1f)) { Text("重启 BL") }
                    Button(onClick = {
                        scope.launch {
                            nativeStatus = null
                            if (!rootMode) {
                                val ok = FlashBridge.rebootViaNative(context, "fastboot")
                                nativeStatus = if (ok) "原生桥已提交：重启到 Fastboot"
                                else "原生桥不可用，请在「设置」开启 Root 模式用 su 执行"
                            } else {
                                engine.writeLine(FlashBridge.cmdSuFastboot(binPath, "reboot-bootloader"))
                                nativeStatus = "已提交（Root 模式：su 执行）"
                            }
                        }
                    }, Modifier.weight(1f)) { Text("重启 FB") }
                }
            }

            // —— 高通 EDL ——
            FlashCard(title = "高通 EDL (QDL)") {
                var loader by remember { mutableStateOf("") }
                OutlinedTextField(loader, { loader = it }, Modifier.fillMaxWidth(), label = { Text("loader 路径（7z 解出的双广/loader 包内文件）") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (loader.isNotBlank()) engine.writeLine(FlashBridge.cmdQdl(loader)) }, Modifier.fillMaxWidth()) { Text("运行 QDL") }
            }

            // —— MTK ——
            FlashCard(title = "联发科 MTK") {
                var scatter by remember { mutableStateOf("") }
                var da by remember { mutableStateOf("") }
                OutlinedTextField(scatter, { scatter = it }, Modifier.fillMaxWidth(), label = { Text("scatter.txt 路径") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(da, { da = it }, Modifier.fillMaxWidth(), label = { Text("DA / Preloader 路径") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (scatter.isNotBlank() && da.isNotBlank()) engine.writeLine(FlashBridge.cmdMtk(scatter, da)) }, Modifier.fillMaxWidth()) { Text("开始刷写") }
            }

            // —— OTA payload ——
            FlashCard(title = "OTA payload.bin") {
                var payload by remember { mutableStateOf("") }
                var out by remember { mutableStateOf("/sdcard/ozj_payload") }
                OutlinedTextField(payload, { payload = it }, Modifier.fillMaxWidth(), label = { Text("payload.bin 路径") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(out, { out = it }, Modifier.fillMaxWidth(), label = { Text("输出目录") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (payload.isNotBlank()) engine.writeLine(FlashBridge.cmdPayloadDumper(payload, out)) }, Modifier.fillMaxWidth()) { Text("解包 payload") }
            }

            // —— 7z 解包 ——
            FlashCard(title = "7z 解包 loader / 线刷包") {
                var archive by remember { mutableStateOf("") }
                var out by remember { mutableStateOf("/sdcard/ozj_loaders") }
                OutlinedTextField(archive, { archive = it }, Modifier.fillMaxWidth(), label = { Text("压缩包路径") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(out, { out = it }, Modifier.fillMaxWidth(), label = { Text("输出目录") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (archive.isNotBlank()) engine.writeLine(FlashBridge.cmdSevenZipExtract(archive, out)) }, Modifier.fillMaxWidth()) { Text("解包") }
            }

            Text(
                "说明：设备需进入对应模式（fastboot / EDL / MTK BROM）并用 USB 连接；终端 PATH 已注入 " +
                    "内置 adb / fastboot / qdl / 7z / payload-dumper-go 等工具。底层协议库" +
                    "（libnativecmd / libqdl / libmtkcmd 等）随包提供，免 root 时经原生桥直连。",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 命令输出日志
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("输出", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    if (log.isEmpty()) {
                        Text("（命令输出将显示在此）", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            items(log) { line -> Text(line, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FlashCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
