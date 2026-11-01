package com.android.ozjflash.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.android.ozjflash.ui.component.bottombar.MainPagerState

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        fun fromValue(value: String): UiMode = when (value) {
            Material.value -> Material
            else -> Miuix
        }

        val DEFAULT_VALUE = Miuix.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }


/** 主界面分页状态：底栏 / 侧栏与四个页面共享 */
val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}
