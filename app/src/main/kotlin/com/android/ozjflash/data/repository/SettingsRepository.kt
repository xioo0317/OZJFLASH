package com.android.ozjflash.data.repository

import android.content.Context
import androidx.core.content.edit
import com.android.ozjflash.ozjApp

/**
 * OZJFLASH 的应用设置存储。
 *
 * 由 KernelSU 的 SettingsRepository 裁剪而来：只保留 UI / 外观相关的项，
 * 彻底移除 root 特性开关（su 兼容、kernel umount、selinux hide、sulog…）
 * 以及所有 shell / JNI 读写。底层为 SharedPreferences。
 */
interface SettingsRepository {
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enableBlur: Boolean
    /** 模糊风格：gaussian = 高斯模糊（默认）；gradient = 渐变模糊。保留兼容旧存档，UI 已无入口 */
    var blurStyle: String
    /** USB 状态胶囊液态玻璃。保留兼容旧存档，现已并入 uiRevamp 总开关 */
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
    /** 界面换新 (Beta) 总开关：COUI 渐进模糊 + 胶囊/圆钮玻璃，并解锁下方子项 */
    var uiRevamp: Boolean
    /** 选项弹窗风格：miui = 原 MIUI 风格下拉；coui = COUI-Kit 菜单 */
    var dialogStyle: String
    /** 顶栏大标题折叠后的对齐：start = 靠左；center = 居中 */
    var collapsedTitleAlign: String
    /** 凝光视效：COUI 选项弹窗增强聚光高光（液态玻璃观感），随界面换新解锁 */
    var glowEffect: Boolean
}

private const val SETTINGS_PREFS = "settings"

class SettingsRepositoryImpl : SettingsRepository {

    private val prefs by lazy {
        ozjApp.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    }

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

    override var uiRevamp: Boolean
        get() = prefs.getBoolean("ui_revamp", false)
        set(value) = prefs.edit { putBoolean("ui_revamp", value) }

    override var dialogStyle: String
        get() = prefs.getString("dialog_style", "miui") ?: "miui"
        set(value) = prefs.edit { putString("dialog_style", value) }

    override var collapsedTitleAlign: String
        get() = prefs.getString("collapsed_title_align", "center") ?: "center"
        set(value) = prefs.edit { putString("collapsed_title_align", value) }

    override var glowEffect: Boolean
        get() = prefs.getBoolean("glow_effect", false)
        set(value) = prefs.edit { putBoolean("glow_effect", value) }
}
