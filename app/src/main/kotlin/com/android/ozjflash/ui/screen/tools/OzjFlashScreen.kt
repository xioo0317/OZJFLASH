package com.android.ozjflash.ui.screen.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.GlassBackButton
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「欧真加线刷」二级页（占位）。
 *
 * 当前仅放标题 + 官方黑色 OPPO 字标 + 功能即将上线的说明，后续在此接入
 * OPPO/一加 的 fastboot / EDL / OTA payload 线刷流程。顶栏带返回键，
 * 按当前 UI 主体分派到 Miuix / Material 两套外壳，与解压页同源。
 */
@Composable
fun OzjFlashScreen(navigator: Navigator) {
    OzjFlashMiuix(navigator)
}

@Composable
private fun OzjFlashMiuix(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior(
        rememberMiuixTopAppBarState(),
        { true },
        androidx.compose.animation.core.spring<Float>(),
        androidx.compose.animation.core.exponentialDecay<Float>(),
    )
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    color = barColor,
                    title = stringResource(R.string.tools_ozjflash),
                    navigationIcon = {
                        GlassBackButton(
                            imageVector = MiuixIcons.Back,
                            onClick = { navigator.pop() },
                            mirrorForRtl = true,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(12.dp),
        ) {
            OzjFlashBody()
        }
    }
}

@Composable
private fun OzjFlashBody() {
    MiuixCardCompat(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 官方黑色 OPPO 字标放在白色圆形底上，深浅主题都清晰可见
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_oppo),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            }
            MiuixTextCompatTitle(stringResource(R.string.tools_ozjflash))
            MiuixTextCompatBody(stringResource(R.string.tools_ozjflash_desc))
            MiuixTextCompatHint(stringResource(R.string.ozjflash_placeholder))
        }
    }
}

/* --------------------------- Miuix 组件封装 --------------------------- */
// 占位页统一使用 Miuix 原生 Card / Text 与配色。

@Composable
private fun MiuixCardCompat(modifier: Modifier, content: @Composable () -> Unit) {
    MiuixCard(modifier = modifier) { content() }
}

@Composable
private fun MiuixTextCompatTitle(text: String) {
    MiuixText(
        text = text,
        fontSize = 20.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurface,
    )
}

@Composable
private fun MiuixTextCompatBody(text: String) {
    MiuixText(
        text = text,
        fontSize = 14.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun MiuixTextCompatHint(text: String) {
    MiuixText(
        text = text,
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}
