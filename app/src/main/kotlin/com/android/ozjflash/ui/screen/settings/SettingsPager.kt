package com.android.ozjflash.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.FlashBridge
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.navigation3.Route
import com.android.ozjflash.ui.theme.ColorMode
import com.android.ozjflash.ui.viewmodel.AppViewModel

/** 设置页支持的行为集合，两个 UI 主体共用，避免各写一遍 */
class SettingsActions internal constructor(
    private val viewModel: AppViewModel,
    private val navigator: Navigator,
) {
    fun setColorMode(mode: ColorMode) = viewModel.setColorMode(mode)
    fun setKeyColor(color: Int) = viewModel.setKeyColor(color)
    fun setMiuixMonet(enabled: Boolean) = viewModel.setMiuixMonet(enabled)
    fun setPaletteStyle(name: String) = viewModel.setPaletteStyle(name)
    fun setColorSpec(name: String) = viewModel.setColorSpec(name)
    fun setBlur(enabled: Boolean) = viewModel.setBlur(enabled)
    fun setFloatingBottomBar(enabled: Boolean) = viewModel.setFloatingBottomBar(enabled)
    fun setFloatingBottomBarBlur(enabled: Boolean) = viewModel.setFloatingBottomBarBlur(enabled)
    fun setNavigationBadge(enabled: Boolean) = viewModel.setNavigationBadge(enabled)
    fun setPageScale(value: Float) = viewModel.setPageScale(value)
    fun setTerminalPty(enabled: Boolean) = viewModel.setTerminalPty(enabled)
    fun setLanguage(code: String) = viewModel.setLanguage(code)
    /** 刷写执行模式：false=免 root 走 USB 原生桥；true=Root 直接 su 执行工具 */
    fun setRootMode(enabled: Boolean) = FlashBridge.setRootMode(enabled)
    fun openAbout() = navigator.push(Route.About)
    fun openColorPalette() = navigator.push(Route.ColorPalette)
    /** 由底栏移入设置页的终端入口 */
    fun openTerminal() = navigator.push(Route.Terminal)
}

@Composable
fun SettingPager(
    viewModel: AppViewModel,
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = androidx.compose.runtime.remember(viewModel, navigator) {
        SettingsActions(viewModel, navigator)
    }

    SettingsMiuix(uiState, actions, bottomInnerPadding, isCurrentPage)
}

/** 主题色候选：0 表示跟随系统动态色 */
internal val keyColorOptions = listOf(
    0 to R.string.settings_color_dynamic,
    0xFF2E7D32.toInt() to R.string.settings_color_green,
    0xFF1565C0.toInt() to R.string.settings_color_blue,
    0xFF6A1B9A.toInt() to R.string.settings_color_purple,
    0xFFC62828.toInt() to R.string.settings_color_red,
    0xFFEF6C00.toInt() to R.string.settings_color_orange,
)

@Composable
internal fun colorLabel(value: Int): String = stringResource(
    keyColorOptions.firstOrNull { it.first == value }?.second ?: R.string.settings_color_custom
)

internal val pageScaleOptions = listOf(0.85f, 0.9f, 1.0f, 1.1f, 1.15f)

internal val colorModeOptions = listOf(
    ColorMode.SYSTEM to R.string.settings_theme_system,
    ColorMode.LIGHT to R.string.settings_theme_light,
    ColorMode.DARK to R.string.settings_theme_dark,
    ColorMode.MONET_SYSTEM to R.string.settings_theme_monet_system,
    ColorMode.MONET_LIGHT to R.string.settings_theme_monet_light,
    ColorMode.MONET_DARK to R.string.settings_theme_monet_dark,
    ColorMode.DARK_AMOLED to R.string.settings_theme_amoled,
)
