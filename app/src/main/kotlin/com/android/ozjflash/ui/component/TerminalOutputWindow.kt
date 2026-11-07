package com.android.ozjflash.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 命令行 / 解包等「输出」区的统一外壳。
 *
 * 保留 macOS 窗口结构：红黄绿交通灯圆点 + 右上 "Terminal" 标题栏 + 标题栏下
 * 分隔线 + 主体留白，输出以等宽字体打印在主体里、可长按选择复制。
 *
 * **不做液态玻璃**：外壳改用原生 Card 组件（Miuix 用 MiuixCard、Material3 用 M3
 * Card），只配一条白色细边框（边缘）与标题栏/主体之间的白色分线；没有磨砂渐变、
 * 彩虹光斑、手绘阴影或高光环，深色模式下也不会出现厚重高光。
 *
 * **双风格差异**：容器用各自体系的原生组件，文字组件按 [LocalUiMode] 分派 ——
 * Miuix 用 MiuixText、Material3 用 M3 Text；除此之外只做「颜色淡化差异」：
 * 标题 / 输出 / 空态文字统一取各自主题 colorScheme 的 onSurface，按 alpha
 * 派生淡化层次，不另设两套排版或配色。
 */
private val TerminalWindowShape = RoundedCornerShape(20.dp)

/** 交通灯圆点配色：贴近 macOS 默认外观 */
private val TrafficRed = Color(0xFFFE6057)
private val TrafficYellow = Color(0xFFFEBD2B)
private val TrafficGreen = Color(0xFF27C940)

private data class WindowPalette(
    val edge: Color,
    val titleBarBg: Color,
    val divider: Color,
    val titleText: Color,
    val outputText: Color,
    val emptyText: Color,
)

@Composable
private fun windowPalette(uiMode: UiMode): WindowPalette {
    val surface = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.surface
    }
    val onSurface = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
    }
    val dark = surface.red + surface.green + surface.blue < 1.5f
    // 白色边缘 / 分线：深色下压低 alpha 避免刺眼，浅色下接近纯白；都是 1dp 细线，
    // 不再是液态玻璃那种厚高光环。
    val edge = if (dark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.90f)
    // 颜色淡化差异：输出正文用完整 onSurface，标题 0.62，空态提示 0.38。
    return WindowPalette(
        edge = edge,
        // 标题栏比主体略深的浅色差 + 一条可见分线，形成 macOS 窗口的上下层次
        titleBarBg = onSurface.copy(alpha = if (dark) 0.06f else 0.035f),
        divider = onSurface.copy(alpha = if (dark) 0.14f else 0.07f),
        titleText = onSurface.copy(alpha = 0.62f),
        outputText = onSurface,
        emptyText = onSurface.copy(alpha = 0.38f),
    )
}

/**
 * Terminal 窗口输出区。
 *
 * @param output 逐行累积的命令输出；为空时显示 [emptyText]（极淡），主体保持留白。
 * @param emptyText 空输出占位文案（各语言资源）。
 */
@Composable
fun TerminalOutputWindow(
    output: String,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    val uiMode = LocalUiMode.current
    val palette = windowPalette(uiMode)
    val isMiuix = uiMode == UiMode.Miuix

    // 白色细边框：原生 Card 组件的自带背景 + 圆角，外沿勾一圈白色细线。
    val cardModifier = modifier
        .fillMaxWidth()
        .border(width = 1.dp, color = palette.edge, shape = TerminalWindowShape)

    val content: @Composable ColumnScope.() -> Unit = {
        TerminalWindowContent(
            output = output,
            emptyText = emptyText,
            palette = palette,
            isMiuix = isMiuix,
        )
    }

    if (isMiuix) {
        MiuixCard(
            modifier = cardModifier,
            cornerRadius = 20.dp,
            content = content,
        )
    } else {
        M3Card(
            modifier = cardModifier,
            shape = TerminalWindowShape,
            content = content,
        )
    }
}

@Composable
private fun ColumnScope.TerminalWindowContent(
    output: String,
    emptyText: String,
    palette: WindowPalette,
    isMiuix: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题栏：左侧红黄绿交通灯，右侧 "Terminal"（背景用 Card 自带色，无渐变玻璃）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(palette.titleBarBg)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrafficLight(TrafficRed)
                TrafficLight(TrafficYellow)
                TrafficLight(TrafficGreen)
            }
            Spacer(Modifier.weight(1f))
            val titleText = stringResource(R.string.terminal_window_title)
            if (isMiuix) {
                MiuixText(
                    text = titleText,
                    color = palette.titleText,
                    fontSize = 17.sp,
                )
            } else {
                Text(
                    text = titleText,
                    color = palette.titleText,
                    fontSize = 17.sp,
                )
            }
        }
        // 标题栏与主体之间的白色分线
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider),
        )
        // 主体：留白 + 等宽可复制输出
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(16.dp),
        ) {
            SelectionContainer {
                val bodyText = output.ifEmpty { emptyText }
                val bodyColor = if (output.isEmpty()) palette.emptyText else palette.outputText
                if (isMiuix) {
                    MiuixText(
                        text = bodyText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = bodyColor,
                    )
                } else {
                    Text(
                        text = bodyText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = bodyColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrafficLight(color: Color) {
    Box(
        Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape),
    )
}
