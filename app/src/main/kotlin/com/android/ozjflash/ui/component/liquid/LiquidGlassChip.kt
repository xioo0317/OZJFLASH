// 悬浮底栏同款「真液态玻璃」——取背景内容折射，改编自 FloatingBottomBar 的实现。
// 依赖：Kyant0/AndroidLiquidGlass 移植到 miuix-blur 的 vibrancy / lens / InnerShadow（Apache 2.0）。

package com.android.ozjflash.ui.component.liquid

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource

/**
 * 当前页面注册的背景内容层（`LayerBackdrop`），由 `BlurredBar` 注入。顶栏上的
 * USB 胶囊 / 返回键 / 问号键据此采样**真实背景内容**做折射，而不是手绘渐变。
 *
 * 为 null（M3 页不走 BlurredBar / 未开模糊 / API 不支持 RenderEffect / 终端页）时，
 * [Modifier.liquidGlassChip] 自动回退为「无厚白高光环」的克制玻璃底。
 */
val LocalLiquidBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 悬浮底栏同款细边缘高光：1dp BloomStroke 双峰描边（随光源方向变化），深色下压低
 * alpha，杜绝旧的 `border(1.1.dp, White 0.95)` 那圈「厚白高光」。
 */
private fun chipHighlight(dark: Boolean): Highlight = Highlight(
    width = 1.dp,
    alpha = if (dark) 0.45f else 0.7f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.10f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

/**
 * 顶栏小型液态玻璃件（USB 胶囊 / 圆形返回键 / 圆形问号键）共用的背景 Modifier。
 *
 * - **真折射**（[LocalLiquidBackdrop] 非空）：[drawBackdrop] 采样页面背景，依次施加
 *   `vibrancy()` 提饱和、`blur()` 柔化、`lens()` 圆角 SDF 折射；边缘用
 *   [chipHighlight] 细描边 + [innerShadow] 内阴影表达玻璃厚度；外圈 [dropShadow]
 *   只在静止态绘制（淡化时由 [showShadow] = false 取消，避免半透明合成层露矩形块）。
 *   按压时 [lens] 折射强度随 pressProgress 增强，复刻悬浮底栏长按拖动那种「活的」玻璃。
 * - **降级**（backdrop 为空）：半透明渐变玻璃底 + 0.6dp 极低 alpha 边线 + [dropShadow]，
 *   没有任何厚白高光环；M3 / 低版本 / 未开模糊自动走这条。
 *
 * [shape] 决定折射 SDF 与描边轮廓（胶囊用 RoundedCornerShape(50)，圆钮用 CircleShape）。
 */
@Composable
fun Modifier.liquidGlassChip(
    shape: Shape,
    dark: Boolean,
    showShadow: Boolean = true,
): Modifier {
    val backdrop = LocalLiquidBackdrop.current
    val specular = remember(dark) { chipHighlight(dark) }

    // 按压缩放折射用：仅在真折射分支里读，降级分支不产生副作用。
    var pressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "chipPress",
    )
    val pressObserver = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                // Initial pass 只观察、不消费，不影响内部的 clickable / Surface 点击。
                val down = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
                pressed = down
            }
        }
    }

    val shadowModifier = if (showShadow) {
        Modifier.dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = 9.dp,
                color = Color.Black,
                alpha = if (dark) 0.30f else 0.12f,
            ),
        )
    } else {
        Modifier
    }

    val glassModifier = if (backdrop != null) {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    padding = maxOf(padding, 24.dp.toPx())
                    vibrancy()
                    blur(4.dp.toPx(), 4.dp.toPx())
                    val boost = 1f + 0.5f * pressProgress
                    lens(
                        refractionHeight = 16.dp.toPx() * boost,
                        refractionAmount = 12.dp.toPx() * boost,
                    )
                },
                highlight = { specular },
                onDrawSurface = {
                    drawRect(if (dark) Color.White.copy(0.06f) else Color.White.copy(0.16f))
                },
            )
            .innerShadow(shape = shape) {
                InnerShadow(
                    radius = 8.dp,
                    color = if (dark) Color.White.copy(0.05f) else Color.Black.copy(0.06f),
                    alpha = 0.9f,
                )
            }
    } else {
        // 降级：克制玻璃底 + 极细边线，深色下也绝不出现厚白高光环。
        val fill = if (dark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(0.12f),
                    Color.White.copy(0.05f),
                    Color.White.copy(0.09f),
                ),
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(0.58f),
                    Color.White.copy(0.34f),
                    Color.White.copy(0.48f),
                ),
            )
        }
        Modifier
            .background(fill, shape)
            .border(0.6.dp, if (dark) Color.White.copy(0.16f) else Color.White.copy(0.42f), shape)
    }

    // 不在此处 clip：dropShadow 的外扩部分不能被裁掉，且各调用方已有自己的形状裁剪
    // （胶囊走 Surface 的 shape，圆钮在 clickable 前 clip(CircleShape)）。
    return this
        .then(shadowModifier)
        .then(pressObserver)
        .then(glassModifier)
}
