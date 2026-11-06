package com.android.ozjflash.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏「圆形液态玻璃」按钮 —— 复刻 HyperOS 二级页那种白色圆钮：
 * 半透明渐变玻璃底 + 顶部球面高光弧 + 白色高光环 + 柔和投影 + 深色实心符号。
 *
 * 与 [com.android.ozjflash.ui.component.usb.UsbStatusPill] 的手绘玻璃同源（不采样背景、
 * 不依赖 backdrop / shader，M3 与 Miuix 两侧都能稳定渲染）。颜色随当前页面 surface 明暗：
 * 浅色 = 近纯白玻璃 + 近黑符号；深色 / 纯黑 = 深灰半透明白 + 近白符号。
 */
private data class GlassPalette(
    val fill: Brush,
    val highlight: Brush,
    val rim: Brush,
    val tint: Color,
    val shadowAlpha: Float,
)

@Composable
private fun glassPalette(dark: Boolean): GlassPalette {
    val surface = when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.surface
    }
    val darkGlass = dark || (surface.red + surface.green + surface.blue < 1.5f)
    return if (darkGlass) {
        GlassPalette(
            fill = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.07f),
                    Color.White.copy(alpha = 0.13f),
                )
            ),
            highlight = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)
            ),
            rim = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.28f),
                )
            ),
            tint = Color(0xFFF2F2F2),
            shadowAlpha = 0.34f,
        )
    } else {
        GlassPalette(
            fill = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.72f),
                    Color.White.copy(alpha = 0.86f),
                )
            ),
            highlight = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0f))
            ),
            rim = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.60f),
                )
            ),
            tint = Color(0xFF1B1B1B),
            shadowAlpha = 0.13f,
        )
    }
}

/** 圆形液态玻璃底；[content] 收到按明暗算好的符号色，自行绘制图标或文字。 */
@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    size: Dp = 40.dp,
    content: @Composable BoxScope.(tint: Color) -> Unit,
) {
    val palette = glassPalette(dark)
    Box(
        modifier = modifier
            .size(size)
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(radius = 9.dp, color = Color.Black, alpha = palette.shadowAlpha),
            )
            .background(palette.fill, CircleShape)
            .background(palette.highlight, CircleShape)
            .border(width = 1.dp, brush = palette.rim, shape = CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(palette.tint)
    }
}

/** 二级页返回「<」圆钮。[mirrorForRtl] 供 Miuix 那种非 auto-mirrored 的返回图标在 RTL 下翻转。 */
@Composable
fun GlassBackButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    dark: Boolean = false,
    size: Dp = 40.dp,
    mirrorForRtl: Boolean = false,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    GlassCircleButton(onClick, modifier, dark, size) { tint ->
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(size * 0.5f)
                .graphicsLayer { if (mirrorForRtl && rtl) scaleX = -1f },
        )
    }
}

/** 顶栏「?」圆钮：无设备连接时替代 USB 胶囊，点击弹出说明书。 */
@Composable
fun GlassHelpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    size: Dp = 40.dp,
) {
    GlassCircleButton(onClick, modifier, dark, size) { tint ->
        Text(
            text = "?",
            color = tint,
            fontSize = (size.value * 0.5f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
