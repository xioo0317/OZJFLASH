package com.android.ozjflash.ui.screen.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.navigation3.Navigator
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 工具种类：仅保留标题资源，二级页当前为占位页（工具功能后续补齐） */
enum class ToolKind(@StringRes val titleRes: Int) {
    FASTBOOT(R.string.tools_fastboot),
    ADB(R.string.tools_adb),
}

/**
 * 工具二级页入口：按当前 UI 主体分派到 Miuix / Material 两套外壳。
 *
 * 当前为占位空白页（与底栏「功能」页一致：顶栏 + 居中「暂无内容」），
 * 具体的 Fastboot / ADB 工具功能后续再补齐。
 */
@Composable
fun ToolPageScreen(navigator: Navigator, tool: ToolKind) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ToolPageHost(navigator, tool, UiMode.Miuix)
        UiMode.Material -> ToolPageHost(navigator, tool, UiMode.Material)
    }
}

@Composable
private fun ToolPageHost(navigator: Navigator, tool: ToolKind, uiMode: UiMode) {
    val background = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.background
    }
    Column(modifier = Modifier.fillMaxSize().background(background)) {
        ToolPageTopBar(title = stringResource(tool.titleRes), uiMode = uiMode, onBack = { navigator.pop() })
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (uiMode) {
                UiMode.Miuix -> MiuixText(
                    text = stringResource(R.string.empty_content),
                    fontSize = 16.sp,
                    color = Color.Gray,
                )
                UiMode.Material -> Text(
                    text = stringResource(R.string.empty_content),
                    fontSize = 16.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}

/** 顶栏：返回键 + 标题（仿「功能」页顶栏，双 UI 补齐） */
@Composable
private fun ToolPageTopBar(title: String, uiMode: UiMode, onBack: () -> Unit) {
    val background = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.background
    }
    val foreground = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
    }
    Column(modifier = Modifier.background(background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = foreground)
            }
            when (uiMode) {
                UiMode.Miuix -> MiuixText(
                    text = title,
                    color = foreground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                UiMode.Material -> Text(
                    text = title,
                    color = foreground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
