package com.android.ozjflash.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.android.ozjflash.ui.component.bottombar.MainPagerState

/** 主界面分页状态：底栏 / 侧栏与四个页面共享 */
val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}
