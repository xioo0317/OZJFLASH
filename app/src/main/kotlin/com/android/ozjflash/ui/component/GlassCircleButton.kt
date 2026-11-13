package com.android.ozjflash.ui.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.liquid.liquidGlassChip
import com.itos.coui.motion.couiPressMotion
import com.itos.coui.motion.couiTouchMotion
import com.itos.coui.motion.rememberCouiPressMotion
import com.itos.coui.motion.rememberCouiTouchMotion
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏「圆形液态玻璃」按钮（返回键 / 说明问号键）—— 与 USB 胶囊共用同一套
 * [liquidGlassChip]「悬浮底栏同款」真液态玻璃：取背景内容做 vibrancy / blur / lens
 * 折射，边缘为细描边 + 内阴影，深色下不再有厚白高光环。
 *
 * backdrop 为空（M3 页 / 低版本 / 关闭模糊）时芯片自动回退为克制玻璃底；
 * 取色随当前页面 surface 明暗：深色 / 纯黑 = 近白符号，浅色 = 近黑符号。
 *
 * **拖拽 / 按住缩放 = COUI-Kit 原厂手势**（[rememberCouiTouchMotion] +
 * [rememberCouiPressMotion]），对齐 ColorOS 17 顶栏按钮观感：
 * - `CouiTouchMotion` 4 条独立 spring 轴（scaleX/scaleY/translationX/translationY）
 *   + rubber 橡皮筋阻尼 + 按拖动方向形变 + 抬手速度回弹；
 * - `CouiPressMotion` 按面积/长宽比自适应按压缩放，与拖拽缩放增量叠加；
 * - 二者均在 PointerEventPass.Initial 只观察不消费，故与末尾 [clickable] 并存。
 * 变换层放在玻璃绘制之前，让玻璃、描边、图标作为整体一起形变，不割裂。
 *
 * [showShadow] 与 USB 胶囊同一份逻辑：折叠顶栏滑动淡化（外层 graphicsLayer 的
 * alpha < 1）时由调用方传 false 取消外阴影，避免半透明合成层露出矩形阴影块。
 */
@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    size: Dp = 40.dp,
    showShadow: Boolean = true,
    dragEnabled: Boolean = true,
    content: @Composable BoxScope.(tint: Color) -> Unit,
) {
    val surface = when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.surface
    }
    val darkGlass = dark || (surface.red + surface.green + surface.blue < 1.5f)
    val tint = if (darkGlass) Color(0xFFF2F2F2) else Color(0xFF1B1B1B)

    // COUI 原厂拖拽（4 轴 spring + rubber 阻尼 + 速度释放）与按住缩放。
    // 公开手势 API 无 @RequiresApi，基于 androidx.dynamicanimation，minSdk 31 可用；
    // 不依赖官方 TopAppBarButton 的 API34 与内部 LocalCouiToolbarBackdrop。
    val motion = rememberCouiTouchMotion(dragEnabled)
    val press = rememberCouiPressMotion()

    Box(
        modifier = modifier
            // 变换层必须在玻璃绘制之前：整个圆钮（玻璃 + 描边 + 图标）一起形变。
            // 缩放增量 = 拖拽形变 + 按压形变 - 1，位移取 rubber 阻尼后的窗口像素。
            .graphicsLayer {
                translationX = motion.translationX.value
                translationY = motion.translationY.value
                scaleX = motion.scaleX.value + press.scale.value - 1f
                scaleY = motion.scaleY.value + press.scale.value - 1f
            }
            .couiTouchMotion(motion, dragEnabled)
            .couiPressMotion(press, dragEnabled)
            .size(size)
            .liquidGlassChip(
                shape = CircleShape,
                dark = darkGlass,
                showShadow = showShadow,
                refract = false,
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(tint)
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
