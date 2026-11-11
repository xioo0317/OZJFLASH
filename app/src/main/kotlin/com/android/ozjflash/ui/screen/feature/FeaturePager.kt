package com.android.ozjflash.ui.screen.feature

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.usb.UsbPillCollapseActions
import com.android.ozjflash.ui.navigation3.LocalNavigator
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.navigation3.Route
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底栏「功能」页：解压工具入口 + 顶栏 USB 状态胶囊。
 *
 * 一张卡片内放两个选项：7z 解压 / payload.bin 解压，行间无分隔线，
 * 左侧图标用主题 onSurface 着色（浅色黑、深色白的黑白配），右侧灰色箭头。
 * 用 Miuix 原生组件渲染卡片，点击后 push 到解压执行页。
 */
@Composable
fun FeaturePager(bottomInnerPadding: Dp, isCurrentPage: Boolean) {
    FeatureMiuix(bottomInnerPadding)
}

@Composable
private fun FeatureMiuix(bottomInnerPadding: Dp) {
    val scrollBehavior = MiuixScrollBehavior(
        rememberMiuixTopAppBarState(),
        { true },
        spring<Float>(),
        exponentialDecay<Float>(),
    )
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.feature),
                    actions = { UsbPillCollapseActions(scrollBehavior) },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                FeatureCardMiuix(navigator)
                Spacer(Modifier.height(12.dp))
                FastbootAdbCardMiuix(navigator)
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }
}

/* -------------------------------- 卡片 -------------------------------- */

private data class FeatureItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val route: Route,
)

private val FEATURE_ITEMS = listOf(
    FeatureItem(
        R.drawable.ic_feature_7z,
        R.string.feature_7z,
        R.string.feature_7z_desc,
        Route.Extract7z,
    ),
    FeatureItem(
        R.drawable.ic_feature_payload,
        R.string.feature_payload,
        R.string.feature_payload_desc,
        Route.ExtractPayload,
    ),
)

@Composable
private fun FeatureCardMiuix(navigator: Navigator) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            FEATURE_ITEMS.forEach { item ->
                FeatureRowMiuix(item) { navigator.push(item.route) }
            }
        }
    }
}

@Composable
private fun FeatureRowMiuix(item: FeatureItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
            modifier = Modifier.size(26.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            MiuixText(
                text = stringResource(item.titleRes),
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
            MiuixText(
                text = stringResource(item.descRes),
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        MiuixIcon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ------------------- Fastboot / Adb 合并卡片（从工具页迁移而来） ------------------- */
/**
 * 工具页原来的 Fastboot、Adb 两张卡片合并为功能页里的一张卡片，两个选项为同一张卡片内的两行，
 * 共享解压卡片的视觉风格（图标随主题着色、右侧灰色箭头、行间无分隔线）。
 */

@Composable
private fun FastbootAdbCardMiuix(navigator: Navigator) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            FastbootAdbRowMiuix(
                icon = Icons.Rounded.FlashOn,
                titleRes = R.string.tools_fastboot,
                descRes = R.string.tools_fastboot_desc,
                onClick = { navigator.push(Route.Fastboot) },
            )
            FastbootAdbRowMiuix(
                icon = Icons.Rounded.Android,
                titleRes = R.string.tools_adb,
                descRes = R.string.tools_adb_desc,
                onClick = { navigator.push(Route.Adb) },
            )
        }
    }
}

@Composable
private fun FastbootAdbRowMiuix(
    icon: ImageVector,
    @StringRes titleRes: Int,
    @StringRes descRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MiuixIcon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            MiuixText(
                text = stringResource(titleRes),
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
            MiuixText(
                text = stringResource(descRes),
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        MiuixIcon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp),
        )
    }
}
