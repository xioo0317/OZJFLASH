package com.android.ozjflash.ui.screen.home

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.usb.UsbStatusPill
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.theme.isInDarkTheme
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior(
        rememberTopAppBarState(),
        { true },
        spring<Float>(),
        exponentialDecay<Float>(),
    )
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                barColor = barColor,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusCard(state = state)
                        InfoCard(state = state, modifier = Modifier.fillMaxWidth())
                        SupportLinks(
                            actions = actions,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    scrollBehavior: ScrollBehavior,
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop?,
    barColor: Color,
) {
    val density = LocalDensity.current
    // Miuix TopAppBar 的 actions 恒定落在「折叠标题行」的垂直中心（CollapsedHeight / 2）；
    // 展开态大标题从 CollapsedHeight 起排，其文字中心 = CollapsedHeight + 大标题行高 / 2。
    // 因此胶囊展开时需向下平移 (折叠行中心 + 大标题行高/2)，折叠后回到 0，正好对齐折叠标题。
    val collapsedCenterPx = with(density) { TopAppBarDefaults.CollapsedHeight.toPx() / 2f }
    // heightOffsetLimit 由 Miuix 测量大标题后写入（负的大标题行高，px）；
    // 首帧是 -Float.MAX_VALUE 哨兵值，用 Title1(32.sp) 默认行高估算兜底，避免跳变。
    val fallbackExpansionPx = with(density) { 32.sp.toPx() * 1.2f }
    BlurredBar(backdrop) {
        TopAppBar(
            color = barColor,
            title = stringResource(R.string.app_name),
            actions = {
                // 胶囊随折叠顶栏移动：展开时对齐大标题（红圈处），折叠后落回折叠标题行。
                // 不再随大标题上移淡出，也不会被顶栏 clipToBounds 裁掉，全程可见。
                // 用 offset 而非 graphicsLayer，保证下拉菜单锚点跟着胶囊一起移动。
                UsbStatusPill(
                    modifier = Modifier.offset {
                        val limit = scrollBehavior.state.heightOffsetLimit
                        val expansion = if (limit < -1f && limit > -10_000f) {
                            -limit
                        } else {
                            fallbackExpansionPx
                        }
                        val fraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
                        IntOffset(
                            x = 0,
                            y = ((collapsedCenterPx + expansion / 2f) * (1f - fraction))
                                .roundToInt(),
                        )
                    }
                )
            },
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
private fun StatusCard(state: HomeUiState) {
    val ready = state.toolsReady
    val scheme = colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = if (ready) {
                if (isInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFAE4)
            } else {
                if (isInDarkTheme()) Color(0xFF3A2A1A) else Color(0xFFFBE9D0)
            },
        ),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(24.dp, 28.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(110.dp),
                    imageVector = if (ready) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline,
                    tint = if (ready) {
                        if (isInDarkTheme()) Color(0xFFB8E8C5) else Color(0xFF36D167)
                    } else {
                        if (isInDarkTheme()) Color(0xFFE0B088) else Color(0xFFE08A2B)
                    },
                    contentDescription = null,
                )
            }
            Column(
                modifier = Modifier.padding(16.dp, 14.dp),
            ) {
                Text(
                    text = stringResource(
                        if (ready) R.string.home_status_title_ready else R.string.home_status_title_missing,
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(1.dp))
                Text(
                    text = stringResource(
                        if (ready) R.string.home_status_ready else R.string.home_status_missing,
                        state.toolsMissing.size,
                    ),
                    fontSize = 15.sp,
                )
                if (!ready) {
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.toolsMissing.joinToString(", "),
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariantSummary,
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_app_version) + ": " + state.appVersion,
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun InfoText(
        icon: ImageVector,
        title: String,
        content: String,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = colorScheme.onSurface,
            )
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = content,
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoText(
                icon = Icons.Filled.Android,
                title = stringResource(R.string.home_android_version),
                content = "${state.androidVersion} (API ${state.sdk})",
            )
            InfoText(
                icon = Icons.Filled.Smartphone,
                title = stringResource(R.string.home_device_model),
                content = state.deviceModel,
            )
            InfoText(
                icon = Icons.Filled.Tag,
                title = stringResource(R.string.home_abi),
                content = state.abi,
            )
            InfoText(
                icon = Icons.Filled.Fingerprint,
                title = stringResource(R.string.home_fingerprint),
                content = state.fingerprint,
            )
            InfoText(
                icon = Icons.Filled.Security,
                title = stringResource(R.string.home_selinux_status),
                content = state.selinuxStatus,
            )
            InfoText(
                icon = Icons.Filled.Memory,
                title = stringResource(R.string.home_seccomp_status),
                content = "Unknown",
            )
        }
    }
}

@Composable
private fun SupportLinks(
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val githubUrl = stringResource(R.string.home_github_url)
    val learnUrl = stringResource(R.string.home_learn_url)
    Card(modifier = modifier) {
        ArrowPreference(
            title = stringResource(R.string.home_support_title),
            summary = stringResource(R.string.home_support_content),
            startAction = {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = stringResource(R.string.home_support_title),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = { actions.onOpenUrl(githubUrl) },
        )
        ArrowPreference(
            title = stringResource(R.string.home_learn_title),
            summary = stringResource(R.string.home_learn_content),
            startAction = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = stringResource(R.string.home_learn_title),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = { actions.onOpenUrl(learnUrl) },
        )
    }
}
