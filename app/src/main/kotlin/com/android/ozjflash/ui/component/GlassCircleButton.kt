package com.android.ozjflash.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.ui.component.liquid.liquidGlassChip
import com.android.ozjflash.ui.theme.LocalUiRevamp
import com.android.ozjflash.ui.theme.isDarkSurface
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏圆形按钮（返回键 / 说明问号键）。
 *
 * 「界面换新 (Beta)」开启时与 USB 胶囊共用同一套 [liquidGlassChip] 液态玻璃；
 * 关闭时退回普通无底色图标按钮（玻璃实现并入总开关，不再单独常驻）。
 *
 * 交互统一走 [circlePressDrag]：按下弹簧缩小、按住可原地拖动一小段、松手弹簧回弹，
 * 拖动未越过 touch slop 时仍视为点击。关闭/开启液态玻璃都共用这一套。
 *
 * [showShadow] 与 USB 胶囊同一份逻辑：折叠顶栏滑动淡化时由调用方传 false 取消
 * 外阴影，避免半透明合成层露出矩形阴影块。
 */
@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    size: Dp = 40.dp,
    showShadow: Boolean = true,
    content: @Composable BoxScope.(tint: Color) -> Unit,
) {
    val revamp = LocalUiRevamp.current
    val surface = MiuixTheme.colorScheme.surface
    val darkGlass = dark || surface.isDarkSurface()
    val interaction = Modifier.circlePressDrag(onClickAction = onClick)

    if (!revamp) {
        // 关闭界面换新：普通图标按钮，无液态玻璃、无底色
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .then(interaction),
            contentAlignment = Alignment.Center,
        ) {
            content(MiuixTheme.colorScheme.onSurface)
        }
        return
    }

    val tint = if (darkGlass) Color(0xFFF2F2F2) else Color(0xFF1B1B1B)
    Box(
        modifier = modifier
            .size(size)
            .liquidGlassChip(
                shape = CircleShape,
                dark = darkGlass,
                showShadow = showShadow,
                refract = false,
            )
            .clip(CircleShape)
            .then(interaction),
        contentAlignment = Alignment.Center,
    ) {
        content(tint)
    }
}

/**
 * 圆钮统一的按压 / 拖拽 / 回弹交互。
 *
 * - 按下：弹簧缩到 0.94，松手弹回原尺寸（带轻微回弹）；
 * - 按住拖动：视觉位移被限制在 [maxDrag] 半径内（原地拖动一小段），布局位置与
 *   点击热区不变，松手后弹簧归零；
 * - 位移未越过 touch slop 视为点击，回调 [onClickAction]；
 * - 用 [semantics] 补回 `Role.Button` 与点击动作，替代原 `clickable` 的无障碍语义。
 */
@Composable
private fun Modifier.circlePressDrag(
    onClickAction: () -> Unit,
    maxDrag: Dp = 16.dp,
): Modifier {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    val bounce = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            translationX = dragX.value
            translationY = dragY.value
        }
        .pointerInput(maxDrag) {
            val limit = maxDrag.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                scope.launch {
                    scale.animateTo(0.94f, spring(stiffness = Spring.StiffnessMediumLow))
                }
                var total = Offset.Zero
                // drag 只在越过 touch slop 后才回调 onDrag，返回值即「是否真的拖动过」
                val dragged = drag(down.id) { change ->
                    total += change.positionChange()
                    val next = Offset(
                        total.x.coerceIn(-limit, limit),
                        total.y.coerceIn(-limit, limit),
                    )
                    scope.launch {
                        dragX.snapTo(next.x)
                        dragY.snapTo(next.y)
                    }
                    change.consume()
                }
                if (!dragged) onClickAction()
                scope.launch { scale.animateTo(1f, bounce) }
                scope.launch {
                    dragX.animateTo(0f, bounce)
                    dragY.animateTo(0f, bounce)
                }
            }
        }
        .semantics {
            role = Role.Button
            onClick(label = null) {
                onClickAction()
                true
            }
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
    showShadow: Boolean = true,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    GlassCircleButton(
        onClick = onClick,
        modifier = modifier,
        dark = dark,
        size = size,
        showShadow = showShadow,
    ) { tint ->
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

/** 顶栏「?」圆钮：点击弹出该命令行工具的使用说明书（ADB / Fastboot 通用）。 */
@Composable
fun GlassHelpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    size: Dp = 40.dp,
    showShadow: Boolean = true,
) {
    GlassCircleButton(
        onClick = onClick,
        modifier = modifier,
        dark = dark,
        size = size,
        showShadow = showShadow,
    ) { tint ->
        Text(
            text = "?",
            color = tint,
            fontSize = (size.value * 0.5f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
