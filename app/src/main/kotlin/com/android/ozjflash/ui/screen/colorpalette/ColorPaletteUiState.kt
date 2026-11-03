package com.android.ozjflash.ui.screen.colorpalette

import androidx.compose.runtime.Immutable
import com.android.ozjflash.R
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.theme.ColorMode
import com.android.ozjflash.ui.viewmodel.AppUiState
import com.android.ozjflash.ui.viewmodel.AppViewModel

/** 主题设置页的只读状态（照搬 KernelSU 的 ColorPaletteUiState） */
@Immutable
data class ColorPaletteUiState(
    val uiState: AppUiState,
    val currentColorMode: ColorMode,
)

/** 主题设置页支持的行为集合，两个 UI 主体共用 */
class ColorPaletteActions internal constructor(
    private val viewModel: AppViewModel,
    private val navigator: Navigator,
) {
    fun back() = navigator.pop()

    /** 主题模式 Tab：0 跟随系统 / 1 浅色 / 2 深色（保持当前的动态取色开关） */
    fun setColorModeIndex(index: Int) {
        val monet = viewModel.uiState.value.appSettings.colorMode.isMonet
        val mode = when (index) {
            0 -> if (monet) ColorMode.MONET_SYSTEM else ColorMode.SYSTEM
            1 -> if (monet) ColorMode.MONET_LIGHT else ColorMode.LIGHT
            else -> if (monet) ColorMode.MONET_DARK else ColorMode.DARK
        }
        viewModel.setColorMode(mode)
    }

    fun setMiuixMonet(enabled: Boolean) = viewModel.setMiuixMonet(enabled)
    fun setKeyColor(color: Int) = viewModel.setKeyColor(color)
    fun setPaletteStyle(name: String) = viewModel.setPaletteStyle(name)
    fun setColorSpec(name: String) = viewModel.setColorSpec(name)
    fun setBlur(enabled: Boolean) = viewModel.setBlur(enabled)
    fun setFloatingBottomBar(enabled: Boolean) = viewModel.setFloatingBottomBar(enabled)
    fun setFloatingBottomBarBlur(enabled: Boolean) = viewModel.setFloatingBottomBarBlur(enabled)
    fun setNavigationBadge(enabled: Boolean) = viewModel.setNavigationBadge(enabled)
    fun setPagerSwipe(enabled: Boolean) = viewModel.setPagerSwipe(enabled)
    fun setPageScale(value: Float) = viewModel.setPageScale(value)
}

/** 当前主题模式在 TabRow / 分段按钮中的下标（纯黑已移除，归入深色） */
internal fun ColorMode.toPaletteIndex(): Int = when (this) {
    ColorMode.SYSTEM, ColorMode.MONET_SYSTEM -> 0
    ColorMode.LIGHT, ColorMode.MONET_LIGHT -> 1
    else -> 2
}

/** 主题色候选：0 表示跟随系统动态色 */
internal val paletteColorEntries = listOf(
    0 to R.string.settings_color_dynamic,
    0xFF2E7D32.toInt() to R.string.settings_color_green,
    0xFF1565C0.toInt() to R.string.settings_color_blue,
    0xFF6A1B9A.toInt() to R.string.settings_color_purple,
    0xFFC62828.toInt() to R.string.settings_color_red,
    0xFFEF6C00.toInt() to R.string.settings_color_orange,
)
