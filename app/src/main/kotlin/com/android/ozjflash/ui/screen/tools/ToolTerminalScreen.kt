package com.android.ozjflash.ui.screen.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 工具二级页背景：与 Termux 上游一致，恒为纯黑，不跟随主题 */
private val ToolTerminalBackground = Color(0xFF000000)
private val ToolTerminalForeground = Color(0xFFE6E6E6)
private val ToolKeyBackground = Color(0xFF1F1F1F)

/** 单个快捷操作：标签 + 发送到终端的命令 */
data class ToolAction(val label: String, val command: String)

/** 工具种类：标题 + 常用操作（点击即向终端发送对应命令，不再自动执行） */
enum class ToolKind(@StringRes val titleRes: Int, val actions: List<ToolAction>) {
    FASTBOOT(
        R.string.tools_fastboot,
        listOf(
            ToolAction("重启", "fastboot reboot"),
            ToolAction("重启系统", "fastboot reboot"),
            ToolAction("重启 Recovery", "fastboot reboot recovery"),
            ToolAction("重启 FastbootD", "fastboot reboot fastboot"),
            ToolAction("Getvar All", "fastboot getvar all"),
            ToolAction("设备列表", "fastboot devices"),
        ),
    ),
    ADB(
        R.string.tools_adb,
        listOf(
            ToolAction("重启", "adb reboot"),
            ToolAction("重启 Recovery", "adb reboot recovery"),
            ToolAction("重启 BL", "adb reboot bootloader"),
            ToolAction("设备列表", "adb devices"),
            ToolAction("截屏", "adb exec-out screencap -p > /sdcard/screen.png"),
            ToolAction("Shell", "adb shell"),
        ),
    ),
}

/** 入口分派：按当前 UI 主体选择 Miuix / Material 两套外壳 */
@Composable
fun ToolPageScreen(navigator: Navigator, tool: ToolKind) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ToolPageHost(navigator, tool, uiMode = UiMode.Miuix)
        UiMode.Material -> ToolPageHost(navigator, tool, uiMode = UiMode.Material)
    }
}

@Composable
private fun ToolPageHost(navigator: Navigator, tool: ToolKind, uiMode: UiMode) {
    val context = LocalContext.current
    val binPath = remember { NativeLibs.prepare(context) }
    val nativeDir = context.applicationInfo.nativeLibraryDir
    val shellPath = remember(binPath) { NativeLibs.shellPath(binPath) }
    val realTerminal = rememberRealTerminal(shellPath, binPath, nativeDir)

    DisposableEffect(realTerminal) {
        onDispose { realTerminal.session.finishIfRunning() }
    }

    Column(modifier = Modifier.fillMaxSize().background(ToolTerminalBackground)) {
        ToolPageTopBar(title = stringResource(tool.titleRes), uiMode = uiMode, onBack = { navigator.pop() })
        ToolPageBody(tool = tool, realTerminal = realTerminal)
    }
}

/** 顶栏：返回键 + 标题 + 右侧 USB 状态胶囊（按 UI 主体选用对应 Text 组件，双 UI 补齐） */
@Composable
private fun ToolPageTopBar(title: String, uiMode: UiMode, onBack: () -> Unit) {
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = ToolTerminalForeground)
        }
        when (uiMode) {
            UiMode.Miuix -> MiuixText(
                text = title,
                color = ToolTerminalForeground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            UiMode.Material -> Text(
                text = title,
                color = ToolTerminalForeground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
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

/** 主体：常用操作胶囊 + 内嵌真终端（不再自动执行指令，全部由按钮触发） */
@Composable
private fun ToolPageBody(tool: ToolKind, realTerminal: RealTerminal) {
    Column(modifier = Modifier.fillMaxSize().background(ToolTerminalBackground)) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tool.actions.forEach { action ->
                ToolActionChip(label = action.label) { realTerminal.sendLine(action.command) }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RealTerminalView(terminal = realTerminal, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ToolActionChip(label: String, onClick: () -> Unit) {
    Surface(
        color = ToolKeyBackground,
        contentColor = ToolTerminalForeground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
