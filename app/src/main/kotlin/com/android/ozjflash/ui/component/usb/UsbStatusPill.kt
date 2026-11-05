package com.android.ozjflash.ui.component.usb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.UsbDeviceInfo
import com.android.ozjflash.flash.UsbHost
import com.android.ozjflash.flash.UsbMode
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏 USB 状态胶囊：右侧一个圆角胶囊按钮，内含状态灰点 + 文案 + 上箭头；
 * 点击弹出下拉（设备列表 / 请求授权 / 刷新列表）。
 *
 * - MIUI（Miuix）皮肤：下拉直接用 Miuix 的 [OverlayDropdownPopup]（与设置页
 *   「界面风格」「语言」那套下拉是同一个组件），不再自绘卡片，观感与 MIUI 系统一致；
 * - M3（Material）皮肤：下拉用 Material3 的 DropdownMenu / DropdownMenuItem。
 *
 * 点设备条目 → 弹出 [UsbDeviceInfoDialog]（设备信息详情）。
 * 设备接入/拔出由 [UsbHost] 的广播接收器自动 refresh，这里挂载时再补一次枚举，
 * 做到「连接设备自动访问」。
 *
 * [dark] 为 true 时按终端纯黑背景配色（浅字 + 半透明深底），否则跟随浅色主题。
 */
@Composable
fun UsbStatusPill(dark: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiMode = LocalUiMode.current
    val devices by UsbHost.devices.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    // 非空时显示设备信息弹窗
    var infoDevice by remember { mutableStateOf<UsbDeviceInfo?>(null) }

    // 挂载即枚举一次，确保进入页面时立刻反映已接入的设备（自动访问）
    LaunchedEffect(Unit) { UsbHost.refresh(context) }

    val granted = devices.count { it.granted }
    val statusText = when {
        devices.isEmpty() -> stringResource(R.string.usb_status_none)
        devices.size == 1 -> stringResource(R.string.usb_status_connected_type, modeShortType(devices[0].mode))
        else -> stringResource(R.string.usb_status_connected, devices.size)
    }

    // 颜色一律取当前皮肤的主题色，跟随深色模式；不再写死浅灰，否则深色下胶囊会发白
    // （dark = true 仅用于终端纯黑背景那条链路，保留一套硬编码深色）
    val pillBg = when {
        dark -> Color(0xFF2A2A2A)
        uiMode == UiMode.Miuix -> MiuixTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val textColor = when {
        dark -> Color(0xFFE6E6E6)
        uiMode == UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = when {
        devices.isEmpty() -> textColor.copy(alpha = 0.4f)
        granted > 0 -> Color(0xFF4CAF50)
        else -> Color(0xFFFFB300)
    }

    val openInfo: (UsbDeviceInfo) -> Unit = {
        expanded = false
        infoDevice = it
    }

    Box(modifier = modifier) {
        // 胶囊本体（外形两种皮肤一致，文案组件随皮肤切换）
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pillBg)
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            when (uiMode) {
                UiMode.Miuix -> MiuixText(statusText, color = textColor, fontSize = 13.sp)
                UiMode.Material -> M3Text(statusText, color = textColor, fontSize = 13.sp)
            }
            Icon(
                Icons.Rounded.ArrowDropUp,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp),
            )
        }

        // 下拉挂在同一个 Box 里：Miuix 的 OverlayListPopup 以父布局边界为锚点，
        // 这样弹层正好贴在胶囊下方、右边缘对齐
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

/** 按连接模式给出用于状态文案的短类型名 */
private fun modeShortType(mode: UsbMode): String = when (mode) {
    UsbMode.ADB -> "ADB"
    UsbMode.FASTBOOT -> "Fastboot"
    UsbMode.EDL, UsbMode.DLOAD -> "EDL"
    UsbMode.MTK_BROM -> "MTK"
    else -> "USB" // QUALCOMM / XIAOMI / OPPO / ONEPLUS / ANDROID_BRAND / NONE
}

/** 下拉里一条设备项的文案：类型 + 小写 VID:PID，如「ADB 22d9:2769」 */
private fun deviceItemText(info: UsbDeviceInfo): String =
    "${modeShortType(info.mode)} ${info.vidPid.lowercase()}"

/**
 * MIUI 风格下拉：直接用 Miuix 的 [OverlayDropdownPopup]（设置页下拉同款组件）。
 * 两组：设备（含「请求 USB 权限」动作）+ 「刷新列表」，与 MIUI 系统的分组下拉一致。
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

    val deviceItems = buildList<DropdownItem> {
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
    }
    val refreshItems = listOf(DropdownItem(text = refreshText, onClick = onRefresh))

    // 单一分组：设备项 + 刷新列表连着排，中间不出现 Miuix 的分组分隔线
    OverlayDropdownPopup(
        entries = listOf(
            DropdownEntry(items = deviceItems + refreshItems),
        ),
        show = expanded,
        onDismiss = onDismiss,
        onDismissFinished = {},
        maxHeight = null,
        dropdownColors = DropdownDefaults.dropdownColors(),
        renderInRootScaffold = true,
        collapseOnSelection = true,
    )
}

/** Material3 风格下拉：沿用 M3 DropdownMenu / DropdownMenuItem */
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
        HorizontalDivider()
        DropdownMenuItem(
            text = { M3Text(stringResource(R.string.usb_refresh_list)) },
            onClick = {
                onRefresh()
                onDismiss()
            },
        )
    }
}
