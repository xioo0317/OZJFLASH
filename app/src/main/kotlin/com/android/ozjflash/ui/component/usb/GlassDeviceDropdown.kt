package com.android.ozjflash.ui.component.usb

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.android.ozjflash.R
import com.android.ozjflash.flash.UsbDeviceInfo
import com.android.ozjflash.ui.component.liquid.liquidGlassChip

/**
 * 「USB 胶囊模糊」开启时，点击胶囊弹出的**液态玻璃下拉卡片**。
 *
 * 与 [UsbStatusPill] 的胶囊共用同一个 [liquidGlassChip] 算法（真折射同款渐变玻璃底 +
 * 细描边 + 内阴影 + 外阴影 + 按压折射增强），只是形状换成卡片圆角；Miuix 自带的
 * [top.yukonga.miuix.kmp.popup.OverlayDropdownPopup] 容器背景是硬编码纯色、且不暴露
 * 容器 modifier，无法替换为玻璃，因此这里自己接管弹出层。
 *
 * **为什么 refract = false**：弹出层是独立窗口，不在页面 LiquidBackdrop 的
 * backdrop 捕获层内，真折射会采样到空/黑纹理（顶栏返回键圆钮已因此关掉折射）。
 * 走同一函数的降级玻璃底分支，视觉与胶囊同一套语言且不会出现黑块。
 *
 * **「裂开」**：卡片以胶囊右上角为原点，按 spring 进度 0→1 从上往下做 clip-reveal
 * 揭开 + 淡入 + 轻微放大，对齐 Miuix 下拉 `popupClipReveal` 的方向感；收起时反向
 * 在 150ms 内合拢。内容不参与缩放，文字不会被压扁。
 *
 * 仅在胶囊模糊开启（Miuix + gradient）时由 [UsbStatusPill] 选用；未开启时仍走原生
 * Miuix 下拉，零回归。
 */
@Composable
internal fun GlassDeviceDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    devices: List<UsbDeviceInfo>,
    onDeviceClick: (UsbDeviceInfo) -> Unit,
    onRequest: (UsbDeviceInfo) -> Unit,
    onRefresh: () -> Unit,
    dark: Boolean,
    contentColor: Color,
) {
    // 进出动画需要 Popup 在收起动画期间仍然存在，用 present 与 expanded 分离控制。
    var present by remember { mutableStateOf(expanded) }
    val progress = remember { Animatable(if (expanded) 1f else 0f) }

    LaunchedEffect(expanded) {
        if (expanded) {
            present = true
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        } else if (present) {
            progress.animateTo(0f, tween<Float>(150))
            present = false
        }
    }

    if (!present) return

    // Animatable 实现 State<T>，在 composable 作用域读一次，保证每帧动画值都驱动重绘
    // （draw 阶段的 lambda 只捕获这个值，不直接订阅动画状态）。
    val revealProgress = progress.value.coerceIn(0f, 1f)

    val noDeviceText = stringResource(R.string.usb_menu_no_device)
    val requestText = stringResource(R.string.usb_request_access)
    val refreshText = stringResource(R.string.usb_refresh_list)

    val items = buildList<GlassMenuItem> {
        if (devices.isEmpty()) {
            add(GlassMenuItem(noDeviceText, enabled = false))
        } else {
            devices.forEach { info ->
                add(GlassMenuItem(deviceItemText(info), onClick = { onDeviceClick(info) }))
                if (!info.granted) {
                    add(GlassMenuItem(requestText, onClick = { onRequest(info) }))
                }
            }
        }
        add(GlassMenuItem(refreshText, onClick = onRefresh))
    }

    // 胶囊高度约 30dp：卡片右端对齐胶囊右端，顶部落在胶囊底部 + 12dp 间隙。
    // 用自定义 PositionProvider 精确右对齐并夹在窗口内，避免 alignment 语义把卡片推到屏外。
    val gapPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    val cardShape = RoundedCornerShape(20.dp)
    val positionProvider = remember(gapPx) {
        object : PopupPositionProvider {
            @Suppress("UNUSED_PARAMETER")
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val maxLeft = (windowSize.width - popupContentSize.width - 16).coerceAtLeast(0)
                val left = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxLeft)
                val top = (anchorBounds.bottom + gapPx)
                    .coerceAtMost(windowSize.height - popupContentSize.height)
                    .coerceAtLeast(0)
                return IntOffset(left, top)
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 288.dp)
                .graphicsLayer {
                    alpha = revealProgress
                    // 从胶囊右上角（锚点）轻微放大长出，配合下方 clip-reveal 形成裂开感
                    scaleX = 0.94f + 0.06f * revealProgress
                    scaleY = 0.94f + 0.06f * revealProgress
                    transformOrigin = TransformOrigin(1f, 0f)
                }
                // 从上往下揭开：只绘制顶部 progress 比例的内容，产生「从胶囊裂开长出」的观感。
                // 用 clipPath + Path（与 Miuix popupClipReveal 同构，已验证可编译）。
                .drawWithContent {
                    val reveal = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height * revealProgress)
                        lineTo(0f, size.height * revealProgress)
                        close()
                    }
                    clipPath(reveal) {
                        this@drawWithContent.drawContent()
                    }
                }
                .liquidGlassChip(
                    shape = cardShape,
                    dark = dark,
                    showShadow = true,
                    refract = false,
                )
                .padding(vertical = 8.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val lastIndex = items.lastIndex
                items.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = item.enabled) {
                                item.onClick?.invoke()
                                onDismiss()
                            }
                            .padding(
                                horizontal = 20.dp,
                                vertical = if (index == 0 || index == lastIndex) 16.dp else 12.dp,
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicText(
                            text = item.text,
                            style = TextStyle(
                                color = if (item.enabled) contentColor else contentColor.copy(alpha = 0.45f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private data class GlassMenuItem(
    val text: String,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
)
