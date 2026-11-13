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
 * 以及所有 shell / JNI 读写。底层为 SharedPreferences，统一收敛到
 * [UiSettingsKeys.PREFS_NAME]（即 settings.xml），key 一律引用 [UiSettingsKeys]。
 */
interface SettingsRepository {
    var uiMode: String
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enableBlur: Boolean
    /** 模糊风格：gaussian = 默认（可调通透度）；gradient = 透明渐变 */
    var blurStyle: String
    /** 「默认」档背景通透度（由设置页进度条自定义，默认 25f，越大约通透） */
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
    /** 折叠标题对齐：false = 靠左，true = 居中（默认靠左） */
    var titleCentered: Boolean
    /** 选项弹窗风格：coui = ColorOS 17 磨砂透背景卡片；miuix = Miuix 原厂下拉（默认 coui） */
    var dropdownStyle: String
}

class SettingsRepositoryImpl : SettingsRepository {

    private val prefs by lazy {
        ozjApp.getSharedPreferences(UiSettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString(UiSettingsKeys.UI_MODE, UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString(UiSettingsKeys.UI_MODE, value) }

    override var themeMode: Int
        get() = prefs.getInt(UiSettingsKeys.COLOR_MODE, 0)
        set(value) = prefs.edit { putInt(UiSettingsKeys.COLOR_MODE, value) }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.MIUIX_MONET, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.MIUIX_MONET, value) }

    override var keyColor: Int
        get() = prefs.getInt(UiSettingsKeys.KEY_COLOR, 0)
        set(value) = prefs.edit { putInt(UiSettingsKeys.KEY_COLOR, value) }

    override var colorStyle: String
        get() = prefs.getString(UiSettingsKeys.COLOR_STYLE, "TonalSpot") ?: "TonalSpot"
        set(value) = prefs.edit { putString(UiSettingsKeys.COLOR_STYLE, value) }

    override var colorSpec: String
        get() = prefs.getString(UiSettingsKeys.COLOR_SPEC, "SPEC_2025") ?: "SPEC_2025"
        set(value) = prefs.edit { putString(UiSettingsKeys.COLOR_SPEC, value) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.ENABLE_BLUR, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.ENABLE_BLUR, value) }

    override var blurStyle: String
        get() = prefs.getString(UiSettingsKeys.BLUR_STYLE, "gaussian") ?: "gaussian"
        set(value) = prefs.edit { putString(UiSettingsKeys.BLUR_STYLE, value) }

    override var blurRadius: Float
        get() = prefs.getFloat(UiSettingsKeys.BLUR_RADIUS, 25f)
        set(value) = prefs.edit { putFloat(UiSettingsKeys.BLUR_RADIUS, value) }

    override var usbPillBlur: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.USB_PILL_BLUR, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.USB_PILL_BLUR, value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.ENABLE_FLOATING_BOTTOM_BAR, true)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.ENABLE_FLOATING_BOTTOM_BAR, value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.ENABLE_FLOATING_BOTTOM_BAR_BLUR, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.ENABLE_FLOATING_BOTTOM_BAR_BLUR, value) }

    override var enableNavigationBadge: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.ENABLE_NAVIGATION_BADGE, true)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.ENABLE_NAVIGATION_BADGE, value) }

    override var navigationRailExpanded: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.NAV_RAIL_EXPANDED, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.NAV_RAIL_EXPANDED, value) }

    override var enablePagerSwipe: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.ENABLE_PAGER_SWIPE, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.ENABLE_PAGER_SWIPE, value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.ENABLE_PREDICTIVE_BACK, true)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.ENABLE_PREDICTIVE_BACK, value) }

    override var pageScale: Float
        get() = prefs.getFloat(UiSettingsKeys.PAGE_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(UiSettingsKeys.PAGE_SCALE, value) }

    override var terminalPty: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.TERMINAL_PTY, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.TERMINAL_PTY, value) }

    override var language: String
        get() = prefs.getString(UiSettingsKeys.LANGUAGE, "system") ?: "system"
        set(value) = prefs.edit { putString(UiSettingsKeys.LANGUAGE, value) }

    override var titleCentered: Boolean
        get() = prefs.getBoolean(UiSettingsKeys.TITLE_CENTERED, false)
        set(value) = prefs.edit { putBoolean(UiSettingsKeys.TITLE_CENTERED, value) }

    override var dropdownStyle: String
        get() = prefs.getString(UiSettingsKeys.DROPDOWN_STYLE, "coui") ?: "coui"
        set(value) = prefs.edit { putString(UiSettingsKeys.DROPDOWN_STYLE, value) }
}
