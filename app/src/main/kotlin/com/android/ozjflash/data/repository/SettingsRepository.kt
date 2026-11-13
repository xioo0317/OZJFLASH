package com.android.ozjflash.data.repository

import android.content.Context
import androidx.core.content.edit
import com.android.ozjflash.ozjApp
import com.android.ozjflash.ui.UiMode

/**
 * OZJFLASH 的应用设置存储。
 *
 * 由 KernelSU 的 SettingsRepository 裁剪而来：只保留 UI / 外观相关的项，
 * 彻底移除 root 特性开关（su 兼容、kernel umount、selinux hide、sulog…）
 * 以及所有 shell / JNI 读写。底层为 SharedPreferences。
 */
interface SettingsRepository {
    var uiMode: String
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enableBlur: Boolean
    /** 模糊风格：gaussian = 默认（可调半径高斯模糊）；gradient = 透明渐变 */
    var blurStyle: String
    /** 「默认」高斯模糊半径（px），由设置页进度条自定义，默认 25f */
    var blurRadius: Float
    /** USB 状态胶囊「液态胶囊」开关（独立于顶/底栏模糊与模糊风格，默认关闭） */
    var usbPillBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var enableNavigationBadge: Boolean
    var navigationRailExpanded: Boolean
    /** 是否允许在主界面左右滑动切换页面（默认关闭，只能用底栏切换，避免与终端/横向手势冲突） */
    var enablePagerSwipe: Boolean
    /** 是否开启系统预测性返回手势（Android 14+，默认开启，需在 Activity 重建后生效） */
    var enablePredictiveBack: Boolean
    var pageScale: Float
    /** 终端是否默认走 PTY（libtermux.so），否则用 ProcessBuilder 管道模式 */
    var terminalPty: Boolean
    /** 应用显示语言：system = 跟随系统，否则为 BCP-47/限定符（zh-CN, ja, es…） */
    var language: String
}

private const val SETTINGS_PREFS = "settings"

class SettingsRepositoryImpl : SettingsRepository {

    private val prefs by lazy {
        ozjApp.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var themeMode: Int
        get() = prefs.getInt("color_mode", 0)
        set(value) = prefs.edit { putInt("color_mode", value) }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean("miuix_monet", false)
        set(value) = prefs.edit { putBoolean("miuix_monet", value) }

    override var keyColor: Int
        get() = prefs.getInt("key_color", 0)
        set(value) = prefs.edit { putInt("key_color", value) }

    override var colorStyle: String
        get() = prefs.getString("color_style", "TonalSpot") ?: "TonalSpot"
        set(value) = prefs.edit { putString("color_style", value) }

    override var colorSpec: String
        get() = prefs.getString("color_spec", "SPEC_2025") ?: "SPEC_2025"
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var blurStyle: String
        get() = prefs.getString("blur_style", "gaussian") ?: "gaussian"
        set(value) = prefs.edit { putString("blur_style", value) }

    override var blurRadius: Float
        get() = prefs.getFloat("blur_radius", 25f)
        set(value) = prefs.edit { putFloat("blur_radius", value) }

    override var usbPillBlur: Boolean
        get() = prefs.getBoolean("usb_pill_blur", false)
        set(value) = prefs.edit { putBoolean("usb_pill_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", true)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var enableNavigationBadge: Boolean
        get() = prefs.getBoolean("enable_navigation_badge", true)
        set(value) = prefs.edit { putBoolean("enable_navigation_badge", value) }

    override var navigationRailExpanded: Boolean
        get() = prefs.getBoolean("nav_rail_expanded", false)
        set(value) = prefs.edit { putBoolean("nav_rail_expanded", value) }

    override var enablePagerSwipe: Boolean
        get() = prefs.getBoolean("enable_pager_swipe", false)
        set(value) = prefs.edit { putBoolean("enable_pager_swipe", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", true)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }

    override var terminalPty: Boolean
        get() = prefs.getBoolean("terminal_pty", false)
        set(value) = prefs.edit { putBoolean("terminal_pty", value) }

    override var language: String
        get() = prefs.getString("language", "system") ?: "system"
        set(value) = prefs.edit { putString("language", value) }
}
