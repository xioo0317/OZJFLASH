package com.android.ozjflash.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * macOS 风格 Terminal 窗口 —— 命令行 / 解包等「输出」区的统一外壳。
 *
 * 还原参考图：大圆角浅色磨砂玻璃窗口 + 左上红黄绿交通灯圆点 + 右上 "Terminal"
 * 标题 + 标题栏细分隔线 + 主体留白（带极淡彩虹散色晕染），输出以等宽字体打印在
 * 主体里、可长按选择复制。空输出时主体保留大块留白（与参考图一致）。
 *
 * 玻璃为纯手绘渐变（不采样背景、不依赖 backdrop / shader），因此 Miuix 与
 * Material3 两侧、明暗主题下都能稳定渲染；与 [GlassCircleButton] 的手绘玻璃同源。
 * 阴影用多层同心圆角矩形 [drawBehind] 手绘，不参与合成层，缩放 / 淡化时不会
 * 退化成矩形阴影块。
 *
 * **双风格差异**：窗口外壳（交通灯、标题栏、圆角玻璃、光斑、阴影）两套完全一致，
 * 文字组件按 [LocalUiMode] 分派 —— Miuix 用 MiuixText、Material3 用 M3 Text（各用
 * 各自体系的组件），除此之外只做「颜色淡化差异」：标题 / 输出 / 空态文字统一取
 * 各自主题 colorScheme 的 onSurface，按 alpha 派生淡化层次，不另设两套排版或配色。
 */
private val TerminalWindowShape = RoundedCornerShape(20.dp)

/** 交通灯圆点配色：贴近 macOS 默认外观 */
private val TrafficRed = Color(0xFFFE6057)
private val TrafficYellow = Color(0xFFFEBD2B)
private val TrafficGreen = Color(0xFF27C940)

private data class WindowPalette(
    val titleBar: Brush,
    val body: Brush,
    val rim: Brush,
    val divider: Color,
    val titleText: Color,
    val outputText: Color,
    val emptyText: Color,
    val shadowAlpha: Float,
    val dark: Boolean,
)

@Composable
private fun windowPalette(uiMode: UiMode): WindowPalette {
    // 窗口外壳（标题栏、主体玻璃、高光环、分隔线）两套完全一致，与 UI 主体无关。
    // 唯一的风格差异是文字颜色：取各自主题 colorScheme 的 onSurface，再按 alpha
    // 派生「标题 / 输出 / 空态」三级淡化层次，不另设 Miuix / Material 两套配色。
    val surface = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.surface
    }
    val onSurface = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
    }
    val dark = surface.red + surface.green + surface.blue < 1.5f
    // 颜色淡化差异：输出正文用完整 onSurface，标题 0.62，空态提示 0.38。
    val titleText = onSurface.copy(alpha = 0.62f)
    val outputText = onSurface
    val emptyText = onSurface.copy(alpha = 0.38f)
    return if (dark) {
        WindowPalette(
            titleBar = Brush.verticalGradient(
                listOf(Color(0xFF2C313A), Color(0xFF242830)),
            ),
            body = Brush.verticalGradient(
                listOf(Color(0xFF1C2027), Color(0xFF171B21)),
            ),
            rim = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.12f),
                ),
            ),
            divider = Color.White.copy(alpha = 0.10f),
            titleText = titleText,
            outputText = outputText,
            emptyText = emptyText,
            shadowAlpha = 0.34f,
            dark = true,
        )
    } else {
        WindowPalette(
            titleBar = Brush.verticalGradient(
                listOf(Color(0xFFECEFF5), Color(0xFFE5E9F0)),
            ),
            body = Brush.verticalGradient(
                listOf(Color(0xFFF2F5FA), Color(0xFFF8FAFD)),
            ),
            rim = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFC9D2DE).copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.80f),
                ),
            ),
            divider = Color(0xFFDAE1E7),
            titleText = titleText,
            outputText = outputText,
            emptyText = emptyText,
            shadowAlpha = 0.13f,
            dark = false,
        )
    }
}

/**
 * 手绘柔和圆角外阴影：先于 [clip] 绘制，因此外扩部分不会被圆角裁掉。
 * 用多层同心圆角矩形 [drawBehind] 实现，圆角半径固定为窗口圆角（20dp）；
 * 不参与合成层，缩放 / 淡化时不会退化成矩形阴影块。
 */
private fun Modifier.softWindowShadow(strength: Float): Modifier = drawBehind {
    val radius = 20.dp.toPx()
    val layers = 8
    for (i in layers downTo 1) {
        val t = i.toFloat() / layers
        val spread = 1.dp.toPx() + t * 6.dp.toPx()
        drawRoundRect(
            color = Color.Black.copy(alpha = strength * (1f - t) * 0.14f),
            topLeft = Offset(-spread, spread * 0.5f + 2.dp.toPx()),
            size = Size(size.width + spread * 2f, size.height + spread * 2f),
            cornerRadius = CornerRadius(radius + spread),
        )
    }
}

/** 主体磨砂玻璃上极淡的彩虹散色光斑（模拟玻璃折射透出的环境色）。 */
private data class GlowSpot(val color: Color, val x: Float, val y: Float, val radius: Float)

private fun Modifier.windowGlow(dark: Boolean): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val alpha = if (dark) 0.09f else 0.20f
    val spots = listOf(
        GlowSpot(Color(0xFFBFD9F2), 0.16f, 0.20f, 0.62f),
        GlowSpot(Color(0xFFF3D7E6), 0.84f, 0.28f, 0.55f),
        GlowSpot(Color(0xFFF6E9C7), 0.26f, 0.82f, 0.55f),
        GlowSpot(Color(0xFFD8D2F0), 0.80f, 0.80f, 0.55f),
        GlowSpot(Color(0xFFCFEDE6), 0.50f, 0.50f, 0.70f),
    )
    spots.forEach { spot ->
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(spot.color.copy(alpha = alpha), Color.Transparent),
                center = Offset(w * spot.x, h * spot.y),
                radius = w * spot.radius,
            ),
        )
    }
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
    // Miuix 主体用 MiuixText、Material3 主体用 M3 Text：各用各自体系的文字组件，
    // 排版参数（字号 / 等宽字体 / 行高 / 颜色）两套完全一致，仅组件归属不同。
    val isMiuix = uiMode == UiMode.Miuix
    Column(
        modifier = modifier
            .fillMaxWidth()
            .softWindowShadow(palette.shadowAlpha)
            .clip(TerminalWindowShape)
            .background(palette.body)
            .windowGlow(palette.dark)
            .border(width = 1.5.dp, brush = palette.rim, shape = TerminalWindowShape),
    ) {
        // 标题栏：左侧红黄绿交通灯，右侧 "Terminal"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(palette.titleBar)
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
        // 标题栏与主体之间的细分隔线
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.divider),
        )
        // 主体：磨砂玻璃留白 + 等宽可复制输出
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
