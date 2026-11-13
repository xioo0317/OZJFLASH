package com.android.ozjflash.ui.screen.tools

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.usb.UsbPillCollapseActions
import com.android.ozjflash.ui.navigation3.LocalNavigator
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.navigation3.Route
import com.android.ozjflash.ui.screen.terminal.TerminalConsole
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import com.android.ozjflash.ui.util.rememberCouiBlurBackdrop
import com.android.ozjflash.ui.util.couiBlurSource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** Miuix 主体的工具箱页：仿主页的可折叠顶栏（标题 + USB 状态胶囊）+ 工具卡片 */
@Composable
fun ToolsMiuix(
    state: ToolsState,
    console: TerminalConsole,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior(
        rememberTopAppBarState(),
        { true },
        spring<Float>(),
        exponentialDecay<Float>(),
    )
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val couiBackdrop = rememberCouiBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            BlurredBar(backdrop, couiBackdrop = couiBackdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.tools),
                    // 与主页 / 功能页 / ADB-Fastboot 二级页共用同一套折叠联动：
                    // 展开随大标题渐出、折叠 spring 快速归位，滑动淡化时取消阴影。
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
        Box(modifier = Modifier.couiBlurSource(couiBackdrop)
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // 欧真加线刷：黑色 OPPO 官方字标卡片，点击进入占位页
                        OzjFlashCardMiuix(navigator = navigator)
                        Spacer(Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }
}

/**
 * 「欧真加线刷」卡片（Miuix）：图标为官方黑色 OPPO 字标，放在白色圆形底上保证深浅主题都清晰；
 * 图标不随主题着色，固定为黑色，符合「黑色 OPPO 官方图标」要求。点击进入占位页。
 */
@Composable
private fun OzjFlashCardMiuix(navigator: Navigator) {
    val colorScheme = MiuixTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { navigator.push(Route.OzjFlash) },
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_oppo),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tools_ozjflash),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.tools_ozjflash_desc),
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
