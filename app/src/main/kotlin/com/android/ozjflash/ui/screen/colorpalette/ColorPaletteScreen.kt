package com.android.ozjflash.ui.screen.colorpalette

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.viewmodel.AppViewModel

/**
 * 主题设置页（照搬 KernelSU 的 ColorPalette 二级页）。
 *
 * 由设置页的「主题」入口 push 进导航栈，包含主题预览、主题模式、
 * 动态取色 / 主题色 / 配色风格与规范、模糊、悬浮底栏、液态玻璃、角标与界面缩放。
 */
@Composable
fun ColorPaletteScreen(
    viewModel: AppViewModel,
    navigator: Navigator,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel, navigator) { ColorPaletteActions(viewModel, navigator) }
    val state = ColorPaletteUiState(
        uiState = uiState,
        currentColorMode = uiState.appSettings.colorMode,
    )

    ColorPaletteScreenMiuix(state, actions)
}
