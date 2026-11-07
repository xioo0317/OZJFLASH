package com.android.ozjflash.ui.navigation3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys for miuix-nav.
 *
 * OZJFLASH 只有主框架（底栏分页）加两个二级页面：
 * 关于页与主题配色页。其余 KernelSU 的 root 相关页面（模块 / 超级用户 /
 * Sulog / 脚本执行…）全部移除。
 *
 * 注意：miuix-nav 的 rememberNavBackStack 用 navBackStackSaver
 * （kotlinx.serialization 多态序列化）保存路由栈，进程重建时恢复。
 * Route 及其子类必须保持 @Serializable，否则保存状态即抛 SerializationException。
 */
@Serializable
sealed interface Route : NavKey, Parcelable {

    @Serializable
    @Parcelize
    data object Main : Route

    @Serializable
    @Parcelize
    data object Feature : Route

    @Serializable
    @Parcelize
    data object Terminal : Route

    @Serializable
    @Parcelize
    data object Flash : Route

    @Serializable
    @Parcelize
    data object Tools : Route

    @Serializable
    @Parcelize
    data object Settings : Route

    @Serializable
    @Parcelize
    data object About : Route

    @Serializable
    @Parcelize
    data object ColorPalette : Route

    @Serializable
    @Parcelize
    data object Fastboot : Route

    @Serializable
    @Parcelize
    data object Adb : Route

    /** 工具页 → 欧真加线刷（占位页，后续接入 OPPO/一加 线刷流程） */
    @Serializable
    @Parcelize
    data object OzjFlash : Route

    /** 功能页 → 7z 解压执行页 */
    @Serializable
    @Parcelize
    data object Extract7z : Route

    /** 功能页 → payload.bin 解压执行页 */
    @Serializable
    @Parcelize
    data object ExtractPayload : Route
}
