package com.android.ozjflash.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import com.android.ozjflash.ui.theme.LocalBlurStyle
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
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
    if (blurActive && backdrop != null && blurStyle == "gradient") {
        // 渐变（渐进）模糊：把顶/底栏竖向切成三段，半径由小到大，得到液态玻璃般的效果
        Box {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.34f)
                        .textureBlur(backdrop = backdrop, shape = RectangleShape, blurRadius = 8f, colors = colors),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.33f)
                        .textureBlur(backdrop = backdrop, shape = RectangleShape, blurRadius = 18f, colors = colors),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.33f)
                        .textureBlur(backdrop = backdrop, shape = RectangleShape, blurRadius = 30f, colors = colors),
                )
            }
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
