package com.android.ozjflash.ui.component.usb

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import kotlin.math.abs
import kotlin.math.roundToInt
import com.android.ozjflash.flash.UsbDeviceInfo
import com.android.ozjflash.flash.UsbHost
import com.android.ozjflash.flash.UsbMode
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.theme.LocalBlurStyle
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.theme.LocalUsbPillBlur
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏 USB 状态胶囊。
 *
 * - **风格对应组件**：MIUI 主体用 Miuix [MiuixSurface]，M3 主体用 Material3 [M3Surface]，
 *   点击反馈由各自组件自带；文字也分别用 MiuixText 与 M3Text。
 * - **主题取色**：容器与文字颜色全部取自当前 colorScheme（浅色=白、深色/纯黑=黑、
 *   动态取色时自动带主题色）。胶囊文案按接入设备类型显示（USB设备 / adb设备 /
 *   Fast boot设备等），无设备显示「未连接设备」；下拉里的设备条目沿用旧版
 *   「短类型 + VID:PID」（如 ADB 22d9:2769），点条目查看设备详情。
 *   胶囊右端不再画展开小三角，尺寸更省。
 * - **液态玻璃（仅 Miuix）**：主题设置「胶囊模糊」开启后，用「半透明渐变玻璃底 +
 *   白色高光环 + 柔和投影」手绘。M3 主体按需求取消液态玻璃实现，回退为普通纯色胶囊。
 *
 * [dark] 为终端页纯黑背景的专属配色开关（避免黑底上出现一块亮色胶囊）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbStatusPill(dark: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiMode = LocalUiMode.current
    val devices by UsbHost.devices.collectAsStateWithLifecycle()
    val pillBlurEnabled = LocalUsbPillBlur.current
    var expanded by remember { mutableStateOf(false) }
    var infoDevice by remember { mutableStateOf<UsbDeviceInfo?>(null) }

    // 挂载即枚举一次，确保进入页面时立刻反映已接入的设备（自动访问）
    LaunchedEffect(Unit) { UsbHost.refresh(context) }

    val connected = devices.isNotEmpty()
    val granted = devices.any { it.granted }
    // 胶囊文案按接入的设备类型显示（USB设备 / adb设备 / Fast boot设备等）；
    // 无设备仍显示「未连接设备」，多设备显示数量，避免文字过长撑破胶囊。
    val statusText = when {
        devices.isEmpty() -> stringResource(R.string.usb_status_none)
        devices.size == 1 -> stringResource(usbModeLabelRes(devices[0].mode))
        else -> stringResource(R.string.usb_status_connected, devices.size)
    }

    // 颜色随主题设置的取色走：白 / 黑 / 纯黑 / 动态取色全部由 colorScheme 决定
    val baseContainer = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surfaceVariant
        UiMode.Material -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val baseContent = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
    }
    val containerColor = if (dark) Color(0xFF2A2A2A) else baseContainer
    val contentColor = if (dark) Color(0xFFE6E6E6) else baseContent

    val dotColor = when {
        !connected -> if (dark) Color(0xFF9AA0A6) else baseContent.copy(alpha = 0.45f)
        granted -> Color(0xFF4CAF50)
        else -> Color(0xFFFFB300)
    }

    // 取当前页面背景色判断明暗，决定玻璃是「透亮白」还是「深色带淡白」
    val glassBase = if (dark) Color(0xFF101010) else when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.surface
    }
    val isDarkSurface = (glassBase.red + glassBase.green + glassBase.blue) < 1.5f
    val darkGlass = dark || isDarkSurface

    // 玻璃底：上缘亮、中部略暗、下缘回亮的半透明渐变，模拟球面玻璃受光。
    val glassFill = if (darkGlass) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.14f),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.68f),
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.56f),
            )
        )
    }
    // 白色高光环：上缘最亮、下缘淡、底缘回亮
    val rimBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.95f),
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.60f),
        )
    )

    val pillShape = RoundedCornerShape(50)
    // 与主题设置页里「USB 胶囊模糊」选项的显隐条件严格一致：模糊开启 + 效果=渐变。
    // M3 主体按需求取消液态玻璃实现，只保留 Miuix 主体的手绘玻璃。
    val blurActive = LocalEnableBlur.current && LocalBlurStyle.current == "gradient"
    val glassEnabled = pillBlurEnabled && blurActive && uiMode == UiMode.Miuix

    val openInfo: (UsbDeviceInfo) -> Unit = {
        expanded = false
        infoDevice = it
    }

    Box(modifier = modifier) {
        // Miuix 玻璃三要素手绘；M3 / 未开启时为普通纯色 Surface。
        val surfaceModifier = if (glassEnabled) {
            Modifier
                .dropShadow(
                    shape = pillShape,
                    shadow = Shadow(
                        radius = 9.dp,
                        color = Color.Black,
                        alpha = if (dark) 0.30f else 0.12f,
                    ),
                )
                .background(glassFill, pillShape)
                .border(width = 1.1.dp, brush = rimBrush, shape = pillShape)
        } else {
            Modifier
        }

        when (uiMode) {
            UiMode.Miuix -> MiuixSurface(
                onClick = { expanded = !expanded },
                modifier = surfaceModifier,
                shape = pillShape,
                color = if (glassEnabled) Color.Transparent else containerColor,
                contentColor = contentColor,
            ) {
                PillContentMiuix(statusText, dotColor, contentColor)
            }

            UiMode.Material -> M3Surface(
                onClick = { expanded = !expanded },
                modifier = surfaceModifier,
                shape = pillShape,
                color = if (glassEnabled) Color.Transparent else containerColor,
                contentColor = contentColor,
                tonalElevation = 0.dp,
            ) {
                PillContentMaterial(statusText, dotColor, contentColor)
            }
        }

        // 下拉挂在同一个 Box 里：Miuix 的 OverlayDropdownPopup 以父布局边界为锚点
        when (uiMode) {
            UiMode.Miuix -> MiuixDeviceDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                devices = devices,
                onDeviceClick = openInfo,
                onRequest = { UsbHost.requestPermission(context, it.device) },
                onRefresh = { UsbHost.refresh(context) },
            )

            UiMode.Material -> MaterialDeviceDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                devices = devices,
                onDeviceClick = openInfo,
                onRequest = { UsbHost.requestPermission(context, it.device) },
                onRefresh = { UsbHost.refresh(context) },
            )
        }
    }

    infoDevice?.let { info ->
        UsbDeviceInfoDialog(info = info, onDismiss = { infoDevice = null })
    }
}

/** 设备类型的本地化名称：adb 设备 / Fast boot 设备 / EDL 设备 / MTK 设备 / USB 设备 */
@StringRes
internal fun usbModeLabelRes(mode: UsbMode): Int = when (mode) {
    UsbMode.ADB -> R.string.usb_mode_adb
    UsbMode.FASTBOOT -> R.string.usb_mode_fastboot
    UsbMode.EDL, UsbMode.DLOAD -> R.string.usb_mode_edl
    UsbMode.MTK_BROM -> R.string.usb_mode_mtk
    else -> R.string.usb_mode_usb
}

/** 下拉里一条设备项的文案：沿用旧版「短类型 + 小写 VID:PID」，如「ADB 22d9:2769」 */
private fun deviceItemText(info: UsbDeviceInfo): String =
    "${modeShortType(info.mode)} ${info.vidPid.lowercase()}"

/** 按连接模式给出短类型名（旧版下拉文案用，不随系统语言变化） */
private fun modeShortType(mode: UsbMode): String = when (mode) {
    UsbMode.ADB -> "ADB"
    UsbMode.FASTBOOT -> "Fastboot"
    UsbMode.EDL, UsbMode.DLOAD -> "EDL"
    UsbMode.MTK_BROM -> "MTK"
    else -> "USB"
}

/**
 * USB 胶囊跟随 Miuix 可折叠顶栏的通用修饰（主页 / 功能页 / ADB 二级页共用）。
 *
 * - **位置**：展开态下移到大标题文字中心（与大标题同行，即截图红圈处），
 *   折叠态回到 actions 原位（与折叠小标题同行）；按 collapsedFraction 线性过渡。
 * - **透明度**：滑动中随大标题一起渐出（最低 0.3，不再一直实心不透明），
 *   完全折叠后随小标题恢复为不透明，保证收回小标题时胶囊始终可见。
 *
 * 位移用 [offset] 而非 graphicsLayer，让下拉菜单锚点跟着胶囊一起移动；
 * alpha 用 graphicsLayer 在 draw 阶段订阅折叠状态，滚动时只重绘不重组。
 * heightOffsetLimit 由 Miuix 测量大标题后写入（负的大标题行高，px），
 * 首帧是 -Float.MAX_VALUE 哨兵值，用 Title1(32.sp) 默认行高兜底避免跳变。
 */
@Composable
fun rememberUsbPillCollapseModifier(scrollBehavior: ScrollBehavior): Modifier {
    val density = LocalDensity.current
    val collapsedCenterPx = with(density) { TopAppBarDefaults.CollapsedHeight.toPx() / 2f }
    val fallbackExpansionPx = with(density) { 32.sp.toPx() * 1.2f }
    return Modifier
        .offset {
            val limit = scrollBehavior.state.heightOffsetLimit
            val expansion = if (limit < -1f && limit > -10_000f) -limit else fallbackExpansionPx
            val fraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
            IntOffset(
                x = 0,
                y = ((collapsedCenterPx + expansion / 2f) * (1f - fraction)).roundToInt(),
            )
        }
        .graphicsLayer {
            val fraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
            alpha = 0.3f + 0.7f * abs(2f * fraction - 1f)
        }
}

/**
 * ADB 页顶栏「?」说明书圆钮的显隐：大标题展开时显示，收起成小标题时淡出，
 * 把顶栏右侧让位给折叠后回落的 USB 胶囊（展开时胶囊已下移，两者不遮挡）。
 */
fun usbHelpCollapseModifier(scrollBehavior: ScrollBehavior): Modifier =
    Modifier.graphicsLayer {
        val fraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
        alpha = (1f - fraction * 2f).coerceIn(0f, 1f)
    }

@Composable
private fun PillContentMiuix(text: String, dotColor: Color, contentColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        MiuixText(text, color = contentColor, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun PillContentMaterial(text: String, dotColor: Color, contentColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        M3Text(text, color = contentColor, fontSize = 13.sp, maxLines = 1)
    }
}

/**
 * MIUI 风格下拉：直接用 Miuix 的 [OverlayDropdownPopup]（设置页下拉同款组件）。
 * 设备与「刷新列表」合并为同一分组，去掉设备中间的分组横线。
 */
@Composable
private fun MiuixDeviceDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    devices: List<UsbDeviceInfo>,
    onDeviceClick: (UsbDeviceInfo) -> Unit,
    onRequest: (UsbDeviceInfo) -> Unit,
    onRefresh: () -> Unit,
) {
    val noDeviceText = stringResource(R.string.usb_menu_no_device)
    val requestText = stringResource(R.string.usb_request_access)
    val refreshText = stringResource(R.string.usb_refresh_list)

    val entries = buildList<DropdownItem> {
        if (devices.isEmpty()) {
            add(DropdownItem(text = noDeviceText, enabled = false))
        } else {
            devices.forEach { info ->
                add(
                    DropdownItem(
                        text = deviceItemText(info),
                        onClick = { onDeviceClick(info) },
                    )
                )
                if (!info.granted) {
                    add(DropdownItem(text = requestText, onClick = { onRequest(info) }))
                }
            }
        }
        add(DropdownItem(text = refreshText, onClick = onRefresh))
    }

    OverlayDropdownPopup(
        entries = listOf(DropdownEntry(items = entries)),
        show = expanded,
        onDismiss = onDismiss,
        onDismissFinished = {},
        maxHeight = null,
        dropdownColors = DropdownDefaults.dropdownColors(),
        renderInRootScaffold = true,
        collapseOnSelection = true,
    )
}

/** Material3 风格下拉：沿用 M3 DropdownMenu / DropdownMenuItem，设备中间不画横线 */
@Composable
private fun MaterialDeviceDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    devices: List<UsbDeviceInfo>,
    onDeviceClick: (UsbDeviceInfo) -> Unit,
    onRequest: (UsbDeviceInfo) -> Unit,
    onRefresh: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(240.dp),
    ) {
        if (devices.isEmpty()) {
            DropdownMenuItem(
                text = { M3Text(stringResource(R.string.usb_menu_no_device)) },
                onClick = onDismiss,
                enabled = false,
            )
        } else {
            devices.forEach { info ->
                DropdownMenuItem(
                    text = { M3Text(deviceItemText(info)) },
                    trailingIcon = {
                        if (info.granted) {
                            M3Text(
                                stringResource(R.string.usb_granted),
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                            )
                        } else {
                            M3TextButton(onClick = { onRequest(info) }) {
                                M3Text(stringResource(R.string.usb_request))
                            }
                        }
                    },
                    onClick = { onDeviceClick(info) },
                )
            }
        }
        DropdownMenuItem(
            text = { M3Text(stringResource(R.string.usb_refresh_list)) },
            onClick = {
                onRefresh()
                onDismiss()
            },
        )
    }
}
