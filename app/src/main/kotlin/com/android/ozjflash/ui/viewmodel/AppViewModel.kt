package com.android.ozjflash.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ozjflash.data.repository.SettingsRepository
import com.android.ozjflash.data.repository.SettingsRepositoryImpl
import com.android.ozjflash.ui.theme.AppSettings
import com.android.ozjflash.ui.theme.ColorMode
import com.android.ozjflash.ui.theme.ThemeController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 主界面状态（由 KernelSU 的 MainActivityUiState 裁剪而来） */
data class AppUiState(
    val appSettings: AppSettings = AppSettings(
        colorMode = ColorMode.SYSTEM,
        keyColor = 0,
        paletteStyle = com.materialkolor.PaletteStyle.TonalSpot,
        colorSpec = com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.SPEC_2025,
    ),
    /** 是否按系统壁纸生成配色 */
    val miuixMonet: Boolean = false,
    val enableBlur: Boolean = false,
    val blurStyle: String = "gaussian",
    /** USB 状态胶囊的液态玻璃效果开关（兼容旧存档，现已并入 uiRevamp） */
    val usbPillBlur: Boolean = false,
    /** 界面换新 (Beta) 总开关 */
    val uiRevamp: Boolean = false,
    /** 选项弹窗风格：miui / coui */
    val dialogStyle: String = "miui",
    /** 顶栏大标题折叠后对齐：start / center */
    val collapsedTitleAlign: String = "center",
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableNavigationBadge: Boolean = true,
    val enablePagerSwipe: Boolean = false,
    val enablePredictiveBack: Boolean = true,
    val pageScale: Float = 1.0f,
    val terminalPty: Boolean = false,
    val language: String = "system",
    val selectedMainPage: Int = 0,
)

/**
 * 应用级 ViewModel：只负责外观设置，不含 root / shell / 网络行为。
 */
class AppViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(
            appSettings = ThemeController.getAppSettings(repo),
            miuixMonet = repo.miuixMonet,
            enableBlur = repo.enableBlur,
            blurStyle = repo.blurStyle,
            usbPillBlur = repo.usbPillBlur,
            uiRevamp = repo.uiRevamp,
            dialogStyle = repo.dialogStyle,
            collapsedTitleAlign = repo.collapsedTitleAlign,
            enableFloatingBottomBar = repo.enableFloatingBottomBar,
            enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur,
            enableNavigationBadge = repo.enableNavigationBadge,
            enablePagerSwipe = repo.enablePagerSwipe,
            enablePredictiveBack = repo.enablePredictiveBack,
            pageScale = repo.pageScale,
            terminalPty = repo.terminalPty,
            language = repo.language,
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun refreshAppSettings() {
        _uiState.value = _uiState.value.copy(appSettings = ThemeController.getAppSettings(repo))
    }

    fun setColorMode(mode: ColorMode) {
        repo.themeMode = mode.value
        _uiState.value = _uiState.value.copy(appSettings = ThemeController.getAppSettings(repo))
    }

    fun setMiuixMonet(enabled: Boolean) {
        repo.miuixMonet = enabled
        _uiState.value = _uiState.value.copy(
            miuixMonet = enabled,
            appSettings = ThemeController.getAppSettings(repo),
        )
    }

    fun setKeyColor(color: Int) {
        repo.keyColor = color
        _uiState.value = _uiState.value.copy(appSettings = ThemeController.getAppSettings(repo))
    }

    fun setPaletteStyle(name: String) {
        repo.colorStyle = name
        _uiState.value = _uiState.value.copy(appSettings = ThemeController.getAppSettings(repo))
    }

    fun setColorSpec(name: String) {
        repo.colorSpec = name
        _uiState.value = _uiState.value.copy(appSettings = ThemeController.getAppSettings(repo))
    }

    fun setBlur(enabled: Boolean) {
        repo.enableBlur = enabled
        _uiState.value = _uiState.value.copy(enableBlur = enabled)
    }

    fun setBlurStyle(style: String) {
        repo.blurStyle = style
        _uiState.value = _uiState.value.copy(blurStyle = style)
    }

    fun setUsbPillBlur(enabled: Boolean) {
        repo.usbPillBlur = enabled
        _uiState.value = _uiState.value.copy(usbPillBlur = enabled)
    }

    fun setUiRevamp(enabled: Boolean) {
        repo.uiRevamp = enabled
        _uiState.value = _uiState.value.copy(uiRevamp = enabled)
    }

    fun setDialogStyle(style: String) {
        repo.dialogStyle = style
        _uiState.value = _uiState.value.copy(dialogStyle = style)
    }

    fun setCollapsedTitleAlign(align: String) {
        repo.collapsedTitleAlign = align
        _uiState.value = _uiState.value.copy(collapsedTitleAlign = align)
    }

    fun setFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.value = _uiState.value.copy(enableFloatingBottomBar = enabled)
    }

    fun setFloatingBottomBarBlur(enabled: Boolean) {
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.value = _uiState.value.copy(enableFloatingBottomBarBlur = enabled)
    }

    fun setNavigationBadge(enabled: Boolean) {
        repo.enableNavigationBadge = enabled
        _uiState.value = _uiState.value.copy(enableNavigationBadge = enabled)
    }

    fun setPageScale(scale: Float) {
        repo.pageScale = scale
        _uiState.value = _uiState.value.copy(pageScale = scale)
    }

    fun setPagerSwipe(enabled: Boolean) {
        repo.enablePagerSwipe = enabled
        _uiState.value = _uiState.value.copy(enablePagerSwipe = enabled)
    }

    fun setTerminalPty(enabled: Boolean) {
        repo.terminalPty = enabled
        _uiState.value = _uiState.value.copy(terminalPty = enabled)
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        repo.enablePredictiveBack = enabled
        _uiState.value = _uiState.value.copy(enablePredictiveBack = enabled)
    }

    fun setLanguage(code: String) {
        repo.language = code
        _uiState.value = _uiState.value.copy(language = code)
    }

    fun setSelectedMainPage(page: Int) {
        if (_uiState.value.selectedMainPage == page) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedMainPage = page)
        }
    }
}
