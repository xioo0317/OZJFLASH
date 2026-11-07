package com.android.ozjflash.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import com.android.ozjflash.ui.component.liquid.LocalLiquidBackdrop
import com.android.ozjflash.ui.theme.LocalBlurStyle
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

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    content: @Composable () -> Unit,
) {
    val blurStyle = LocalBlurStyle.current
    val colors = BlurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f)),
        ),
    )
    // 把页面注册的背景内容层注入顶栏：顶栏上的 USB 胶囊 / 返回键 / 问号键据此做
    // drawBackdrop 真折射（取背景内容，而非手绘渐变）。M3 页不走 BlurredBar，
    // LocalLiquidBackdrop 为 null，芯片组件自动回退为无厚白高光环的克制玻璃底。
    CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
        if (blurActive && backdrop != null && blurStyle == "gradient") {
            // 渐变（渐进）模糊：使用 Miuix 内置 progressiveTextureBlur + ProgressiveBlur.Top。
            // 模糊半径随高度平滑过渡、底部自然淡出，既不会拉伸铺满整屏，也不会出现生硬的分界线。
            Box(
                modifier = Modifier.progressiveTextureBlur(
                    backdrop,
                    RectangleShape,
                    25f,
                    ProgressiveBlur.Top,
                    colors = colors,
                ),
            ) {
                content()
            }
        } else {
            Box(
                modifier = if (blurActive && backdrop != null) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = 25f,
                        colors = colors,
                    )
                } else {
                    Modifier
                },
            ) {
                content()
            }
        }
    }
}
