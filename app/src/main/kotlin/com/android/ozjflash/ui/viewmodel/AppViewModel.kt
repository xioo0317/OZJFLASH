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
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableNavigationBadge: Boolean = true,
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
            enableFloatingBottomBar = repo.enableFloatingBottomBar,
            enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur,
            enableNavigationBadge = repo.enableNavigationBadge,
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

    fun setTerminalPty(enabled: Boolean) {
        repo.terminalPty = enabled
        _uiState.value = _uiState.value.copy(terminalPty = enabled)
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
