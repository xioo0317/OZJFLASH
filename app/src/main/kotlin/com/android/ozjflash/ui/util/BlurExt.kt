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
import com.android.ozjflash.ui.theme.LocalBlurStyle
import com.android.ozjflash.ui.theme.LocalUiRevamp
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
 * 顶栏统一模糊容器。
 *
 * [couiBackdrop] 非空且「界面换新」开启、系统为 API 33+ 时，走 COUI-Kit 的
 * 渐进高斯模糊（RuntimeShader 原厂曲线）；否则回退到 Miuix 的
 * progressiveTextureBlur / textureBlur。
 *
 * 注意：COUI 的源挂在页面正文节点（[Modifier.revampSource]），本容器只是源外的
 * 消费者；两者是 Scaffold 的 topBar / content 兄弟槽位，不会自采样。
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
    val revamp = LocalUiRevamp.current
    val colors = BlurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f)),
        ),
    )
    // 玻璃胶囊 / 圆钮的真折射仍取 Miuix LayerBackdrop（页面正文层），与 COUI 采样互不干扰
    CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
        val couiActive = revamp && couiBackdrop != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val modifier = when {
            couiActive -> {
                val density = LocalDensity.current
                Modifier.drawCouiGradientBackdrop(
                    backdrop = couiBackdrop!!,
                    fadeHeightPx = 220.dp.value * density.density,
                    referenceBlurRadiusPx = 20.dp.value * density.density,
                )
            }
            blurActive && backdrop != null && blurStyle == "gradient" ->
                Modifier.progressiveTextureBlur(
                    backdrop,
                    RectangleShape,
                    25f,
                    ProgressiveBlur.Top,
                    colors = colors,
                )
            blurActive && backdrop != null ->
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 25f,
                    colors = colors,
                )
            else -> Modifier
        }
        Box(modifier = modifier) {
            content()
        }
    }
}
