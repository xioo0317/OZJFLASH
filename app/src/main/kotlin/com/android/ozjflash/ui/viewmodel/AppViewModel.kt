package com.android.ozjflash.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ozjflash.data.repository.SettingsRepository
import com.android.ozjflash.data.repository.SettingsRepositoryImpl
import com.android.ozjflash.ui.UiMode
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
    val uiMode: UiMode = UiMode.fromValue(UiMode.DEFAULT_VALUE),
    /** Miuix 主体下是否按系统壁纸生成配色（Material 主体恒由 Material You 决定） */
    val miuixMonet: Boolean = false,
    val enableBlur: Boolean = false,
    val blurStyle: String = "gaussian",
    /** 「默认」高斯模糊半径（px），设置页进度条自定义 */
    val blurRadius: Float = 25f,
    /** USB 状态胶囊「液态胶囊」开关（独立于顶/底栏模糊与模糊风格） */
    val usbPillBlur: Boolean = false,
    /** 折叠标题对齐：false = 靠左，true = 居中（默认靠左） */
    val titleCentered: Boolean = false,
    /** 选项弹窗风格：coui = ColorOS 17 卡片；miuix = Miuix 下拉 */
    val dropdownStyle: String = "coui",
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
 * 应用级 ViewModel：只负责外观设置与 UI 模式切换，不含 root / shell / 网络行为。
 */
class AppViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(
            appSettings = ThemeController.getAppSettings(repo),
            uiMode = UiMode.fromValue(repo.uiMode),
            miuixMonet = repo.miuixMonet,
            enableBlur = repo.enableBlur,
            blurStyle = repo.blurStyle,
            blurRadius = repo.blurRadius,
            usbPillBlur = repo.usbPillBlur,
            titleCentered = repo.titleCentered,
            dropdownStyle = repo.dropdownStyle,
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

    fun setUiMode(mode: UiMode) {
        repo.uiMode = mode.value
        _uiState.value = _uiState.value.copy(
            uiMode = mode,
            appSettings = ThemeController.getAppSettings(repo)
        )
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

    fun setBlurRadius(value: Float) {
        repo.blurRadius = value
        _uiState.value = _uiState.value.copy(blurRadius = value)
    }

    fun setUsbPillBlur(enabled: Boolean) {
        repo.usbPillBlur = enabled
        _uiState.value = _uiState.value.copy(usbPillBlur = enabled)
    }

    fun setTitleCentered(enabled: Boolean) {
        repo.titleCentered = enabled
        _uiState.value = _uiState.value.copy(titleCentered = enabled)
    }

    fun setDropdownStyle(style: String) {
        repo.dropdownStyle = style
        _uiState.value = _uiState.value.copy(dropdownStyle = style)
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
