package com.android.ozjflash.ui.screen.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.terminal.RealTerminal
import com.android.ozjflash.terminal.RealTerminalView
import com.android.ozjflash.terminal.rememberRealTerminal
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.usb.UsbStatusPill
import com.android.ozjflash.ui.navigation3.Navigator
import kotlinx.coroutines.delay

/** 工具二级页背景：与 Termux 上游一致，恒为纯黑，不跟随主题 */
private val ToolTerminalBackground = Color(0xFF000000)
private val ToolTerminalForeground = Color(0xFFE6E6E6)

/** 工具种类：标题 + 进入时自动执行的状态指令 */
enum class ToolKind(@StringRes val titleRes: Int, val initialCommand: String) {
    FASTBOOT(R.string.tools_fastboot, "fastboot devices"),
    ADB(R.string.tools_adb, "adb devices"),
}

/** 工具二级页入口：Miuix / Material 共用同一套真终端，仅主体外壳不同 */
@Composable
fun ToolTerminalScreen(navigator: Navigator, tool: ToolKind) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ToolTerminalPage(navigator, tool)
        UiMode.Material -> ToolTerminalPage(navigator, tool)
    }
}

@Composable
private fun ToolTerminalPage(navigator: Navigator, tool: ToolKind) {
    val context = LocalContext.current
    val binPath = remember { NativeLibs.prepare(context) }
    val nativeDir = context.applicationInfo.nativeLibraryDir
    val shellPath = remember(binPath) { NativeLibs.shellPath(binPath) }
    val realTerminal = rememberRealTerminal(shellPath, binPath, nativeDir)

    // 离开页面时回收子进程
    DisposableEffect(realTerminal) {
        onDispose { realTerminal.session.finishIfRunning() }
    }

    // 进入后自动跑一条状态指令，立刻看到设备列表
    LaunchedEffect(realTerminal) {
        delay(300)
        runCatching { realTerminal.sendLine(tool.initialCommand) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ToolTerminalBackground),
    ) {
        ToolTopBar(
            title = stringResource(tool.titleRes),
            onBack = { navigator.pop() },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ToolTerminalBackground),
        ) {
            RealTerminalView(terminal = realTerminal, modifier = Modifier.fillMaxSize())
        }
    }
}

/** 工具页顶栏：返回键 + 标题 + 右侧 USB 状态胶囊（搬到顶栏，与参考图一致） */
@Composable
private fun ToolTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ToolTerminalBackground)
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = ToolTerminalForeground,
            )
        }
        Text(
            text = title,
            color = ToolTerminalForeground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            UsbStatusPill(dark = true)
        }
    }
}
