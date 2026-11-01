package com.android.ozjflash.ui.navigation3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys for miuix-nav.
 *
 * OZJFLASH 只有主框架（底栏分页）加两个二级页面：
 * 关于页与主题配色页。其余 KernelSU 的 root 相关页面（模块 / 超级用户 /
 * Sulog / 脚本执行…）全部移除。
 */
sealed interface Route : NavKey, Parcelable {

    @Parcelize
    data object Main : Route

    @Parcelize
    data object Terminal : Route

    @Parcelize
    data object Flash : Route

    @Parcelize
    data object Tools : Route

    @Parcelize
    data object Settings : Route

    @Parcelize
    data object About : Route

    @Parcelize
    data object ColorPalette : Route
}
