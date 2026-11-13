package com.android.ozjflash.data.repository

/**
 * UI 设置 key 的**唯一事实源**。
 *
 * 所有外观 / 交互开关统一收敛到 [PREFS_NAME] 这一个 SharedPreferences（即
 * `settings.xml`），为将来的「导出 / 恢复 UI 设置」铺路：
 *  - [PREFS_NAME]：唯一存储文件名；
 *  - [ALL]：当前全部持久化 key 的清单，导出时据此遍历快照、恢复时据此反写。
 *
 * 导出 / 恢复 UI 本身暂不实现（按需求只留数据出口），任何新增开关都必须先在这里
 * 登记 key 并同步加入 [ALL]，避免设置散落回多个文件。
 */
object UiSettingsKeys {
    const val PREFS_NAME = "settings"

    const val UI_MODE = "ui_mode"
    const val COLOR_MODE = "color_mode"
    const val MIUIX_MONET = "miuix_monet"
    const val KEY_COLOR = "key_color"
    const val COLOR_STYLE = "color_style"
    const val COLOR_SPEC = "color_spec"
    const val ENABLE_BLUR = "enable_blur"
    const val BLUR_STYLE = "blur_style"
    const val BLUR_RADIUS = "blur_radius"
    const val USB_PILL_BLUR = "usb_pill_blur"
    const val ENABLE_FLOATING_BOTTOM_BAR = "enable_floating_bottom_bar"
    const val ENABLE_FLOATING_BOTTOM_BAR_BLUR = "enable_floating_bottom_bar_blur"
    const val ENABLE_NAVIGATION_BADGE = "enable_navigation_badge"
    const val NAV_RAIL_EXPANDED = "nav_rail_expanded"
    const val ENABLE_PAGER_SWIPE = "enable_pager_swipe"
    const val ENABLE_PREDICTIVE_BACK = "enable_predictive_back"
    const val PAGE_SCALE = "page_scale"
    const val TERMINAL_PTY = "terminal_pty"
    const val LANGUAGE = "language"
    const val TITLE_CENTERED = "title_centered"
    const val DROPDOWN_STYLE = "dropdown_style"

    /** 导出 / 恢复 UI 设置时遍历的完整 key 清单（按声明顺序）。 */
    val ALL: List<String> = listOf(
        UI_MODE, COLOR_MODE, MIUIX_MONET, KEY_COLOR, COLOR_STYLE, COLOR_SPEC,
        ENABLE_BLUR, BLUR_STYLE, BLUR_RADIUS, USB_PILL_BLUR,
        ENABLE_FLOATING_BOTTOM_BAR, ENABLE_FLOATING_BOTTOM_BAR_BLUR,
        ENABLE_NAVIGATION_BADGE, NAV_RAIL_EXPANDED,
        ENABLE_PAGER_SWIPE, ENABLE_PREDICTIVE_BACK, PAGE_SCALE, TERMINAL_PTY,
        LANGUAGE, TITLE_CENTERED, DROPDOWN_STYLE,
    )
}
