package com.android.ozjflash.ui.util

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.ozjflash.ui.component.liquid.LocalLiquidBackdrop
import com.android.ozjflash.ui.theme.LocalBlurRadius
import com.android.ozjflash.ui.theme.LocalBlurStyle
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.effect.drawCouiGradientBackdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 顶栏 / 底栏统一模糊容器。
 *
 * - **透明渐变**（[LocalBlurStyle] = "gradient"）：优先走 COUI-Kit 的
 *   `drawCouiGradientBackdrop`——RuntimeShader 原厂渐进高斯，顶部强模糊、向下平滑
 *   渐隐到全透明。需要页面正文通过 [Modifier.couiBlurSource] 挂同一个
 *   [CouiBackdrop] 源（顶栏为源外消费者），且系统 API 33+；不满足条件自动回退
 *   Miuix `progressiveTextureBlur`。
 * - **默认**（"gaussian"）：Miuix `textureBlur`，半径由 [LocalBlurRadius] 提供，
 *   设置页进度条可实时自定义。
 *
 * [couiBackdrop] 由页面显式传入（默认 null，旧调用点无需改动）。玻璃胶囊 / 圆钮的
 * 真折射仍取 Miuix [LayerBackdrop]，与 COUI 采样互不干扰。
 */
@SuppressLint("NewApi")
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    couiBackdrop: CouiBackdrop? = null,
    blurActive: Boolean = true,
    content: @Composable () -> Unit,
) {
    val blurStyle = LocalBlurStyle.current
    val blurRadius = LocalBlurRadius.current
    val density = LocalDensity.current
    val colors = BlurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f)),
        ),
    )
    val couiSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // 把页面注册的背景内容层注入顶栏：顶栏上的 USB 胶囊 / 返回键 / 问号键据此做
    // drawBackdrop 真折射（取背景内容，而非手绘渐变）。
    CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
        val modifier = when {
            // 透明渐变：COUI 原厂渐进透明高斯（源在页面正文，顶栏是源外消费者）
            blurActive && couiBackdrop != null && blurStyle == "gradient" && couiSupported ->
                Modifier.drawCouiGradientBackdrop(
                    backdrop = couiBackdrop,
                    fadeHeightPx = with(density) { 220.dp.toPx() },
                    referenceBlurRadiusPx = with(density) { 20.dp.toPx() },
                )
            // 透明渐变降级：无 COUI 源 / API<33 时保留 Miuix 渐进模糊观感
            blurActive && backdrop != null && blurStyle == "gradient" ->
                Modifier.progressiveTextureBlur(
                    backdrop,
                    RectangleShape,
                    25f,
                    ProgressiveBlur.Top,
                    colors = colors,
                )
            // 默认高斯：半径由设置页进度条自定义
            blurActive && backdrop != null ->
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = blurRadius,
                    colors = colors,
                )
            else -> Modifier
        }
        Box(modifier = modifier) {
            content()
        }
    }
}
