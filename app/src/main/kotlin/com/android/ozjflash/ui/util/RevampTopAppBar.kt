package com.android.ozjflash.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.ozjflash.ui.theme.LocalCollapsedTitleAlign
import com.android.ozjflash.ui.theme.LocalUiRevamp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 界面换新 (Beta)：支持「标题折叠后靠左 / 居中」的顶栏包装。
 *
 * Miuix `TopAppBar` 把折叠后的小标题硬编码居中（内部 `baseX = (maxWidth - titleWidth)/2`），
 * 且没有任何对齐参数，`titlePadding` 在计算中会被数学抵消，无法靠传参改变。
 *
 * 因此：
 * - **居中（默认）**：完全委托原生 [MiuixTopAppBar]，零改动、零风险；
 * - **靠左**：把库内小标题染成透明（`titleColor` 只作用于小标题，大标题用独立的
 *   `largeTitleColor`），再叠一层自绘小标题，随 `collapsedFraction` 渐显。展开态
 *   大标题仍由库绘制并靠左，折叠态自绘标题在去掉状态栏高度的区域内垂直居中，
 *   与库折叠栏中心对齐。
 *
 * 各页面通过 `import ...RevampTopAppBar as TopAppBar`（或 `as MiuixTopAppBar`）接入，
 * 调用处无需改动。
 */
@Composable
fun RevampTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    // 二级页有返回键时置 true：靠左自绘标题需让出导航区，否则会压在返回键上
    hasNavigationIcon: Boolean = false,
) {
    // 无 scrollBehavior 的页面不会折叠大标题，「折叠后对齐」无意义，直接走原生分支
    val alignStart = LocalUiRevamp.current &&
        LocalCollapsedTitleAlign.current == "start" &&
        scrollBehavior != null

    if (!alignStart) {
        MiuixTopAppBar(
            title = title,
            modifier = modifier,
            color = color,
            titleColor = titleColor,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
        return
    }

    Box(modifier = modifier) {
        MiuixTopAppBar(
            title = title,
            color = color,
            titleColor = Color.Transparent,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )

        // 与库内 smallTitleVisible 同源：只在滚动折叠后渐显，展开态不叠加
        val collapsed by remember(scrollBehavior) {
            derivedStateOf { scrollBehavior?.state?.collapsedFraction ?: 0f }
        }
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // 原生 TitlePadding 是 26dp；有返回键时再让出 48dp（40dp 圆钮 + 8dp 间距），
        // 否则靠左标题会直接压在返回键上。宁可多留白也不能遮挡可点击区域。
        val startPadding = if (hasNavigationIcon) 26.dp + 48.dp else 26.dp
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = statusBarTop)
                .padding(start = startPadding, end = 26.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = MiuixTheme.textStyles.title3.fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = collapsed },
            )
        }
    }
}
