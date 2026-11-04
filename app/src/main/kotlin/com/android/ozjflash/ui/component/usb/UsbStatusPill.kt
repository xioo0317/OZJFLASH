package com.android.ozjflash.ui.component.usb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.UsbDeviceInfo
import com.android.ozjflash.flash.UsbHost
import com.android.ozjflash.flash.UsbMode
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏 USB 状态胶囊：右侧一个圆角胶囊按钮，内含状态灰点 + 文案 + 上箭头；
 * 点击弹出下拉（设备列表 / 请求授权 / 刷新列表）。
 *
 * - MIUI（Miuix）皮肤：下拉用 Miuix 组件（Card + Popup 自绘），契合 MIUI 视觉；
 * - M3（Material）皮肤：下拉用 Material3 的 DropdownMenu / DropdownMenuItem。
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
    var pillHeight by remember { mutableIntStateOf(0) }

    // 挂载即枚举一次，确保进入页面时立刻反映已接入的设备（自动访问）
    LaunchedEffect(Unit) { UsbHost.refresh(context) }

    val granted = devices.count { it.granted }
    val (dotColor, statusText) = when {
        devices.isEmpty() ->
            (if (dark) Color(0xFF9AA0A6) else Color(0xFF9E9E9E)) to
                stringResource(R.string.usb_status_none)
        devices.size == 1 -> {
            val t = modeShortType(devices[0].mode)
            (if (granted > 0) Color(0xFF4CAF50) else Color(0xFFFFB300)) to
                stringResource(R.string.usb_status_connected_type, t)
        }
        else ->
            (if (granted > 0) Color(0xFF4CAF50) else Color(0xFFFFB300)) to
                stringResource(R.string.usb_status_connected, devices.size)
    }

    val pillBg = if (dark) Color(0xFF2A2A2A) else Color(0xFFE8EAED)
    val textColor = if (dark) Color(0xFFE6E6E6) else Color(0xFF1F1F1F)

    Box(modifier = modifier) {
        // 胶囊本体（外形两种皮肤一致，文案组件随皮肤切换）
        Row(
            modifier = Modifier
                .onGloballyPositioned { pillHeight = it.size.height }
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

        when (uiMode) {
            UiMode.Miuix -> MiuixDeviceDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                devices = devices,
                dark = dark,
                pillHeight = pillHeight,
            )

            UiMode.Material -> MaterialDeviceDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                devices = devices,
            )
        }
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

/** 连接模式对应的标识颜色（MIUI 下拉复用） */
private fun modeColor(mode: UsbMode): Color = when (mode) {
    UsbMode.EDL, UsbMode.DLOAD, UsbMode.MTK_BROM -> Color(0xFFE5534B)
    UsbMode.FASTBOOT, UsbMode.ADB -> Color(0xFF3DDC84)
    UsbMode.QUALCOMM, UsbMode.XIAOMI, UsbMode.OPPO, UsbMode.ONEPLUS, UsbMode.ANDROID_BRAND -> Color(0xFF5B9BD5)
    else -> Color(0xFF9AA0A6)
}

/** MIUI 风格下拉：Miuix Card + Popup，右对齐锚定在胶囊下方 */
@Composable
private fun MiuixDeviceDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    devices: List<UsbDeviceInfo>,
    dark: Boolean,
    pillHeight: Int,
) {
    if (!expanded) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val offsetY = pillHeight + with(density) { 6.dp.roundToPx() }

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        MiuixCard(
            modifier = Modifier
                .width(220.dp)
                .padding(8.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (devices.isEmpty()) {
                    MiuixText(
                        text = stringResource(R.string.usb_menu_no_device),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                } else {
                    devices.forEach { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                MiuixText(
                                    text = info.label,
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                )
                                MiuixText(
                                    text = info.vidPid,
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                                MiuixText(
                                    text = info.mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = modeColor(info.mode),
                                )
                            }
                            if (info.granted) {
                                MiuixText(
                                    text = stringResource(R.string.usb_granted),
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.primary,
                                )
                            } else {
                                MiuixTextButton(
                                    text = stringResource(R.string.usb_request),
                                    onClick = {
                                        UsbHost.requestPermission(context, info.device)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.3f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            UsbHost.refresh(context)
                            onDismiss()
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    MiuixText(
                        text = stringResource(R.string.usb_refresh_list),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Material3 风格下拉：沿用 M3 DropdownMenu / DropdownMenuItem */
@Composable
private fun MaterialDeviceDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    devices: List<UsbDeviceInfo>,
) {
    val context = LocalContext.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
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
                    text = { M3Text(info.label) },
                    trailingIcon = {
                        if (info.granted) {
                            M3Text(
                                stringResource(R.string.usb_granted),
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                            )
                        } else {
                            M3TextButton(onClick = {
                                UsbHost.requestPermission(context, info.device)
                                onDismiss()
                            }) { M3Text(stringResource(R.string.usb_request)) }
                        }
                    },
                    onClick = {
                        if (!info.granted) UsbHost.requestPermission(context, info.device)
                        onDismiss()
                    },
                )
            }
        }
        HorizontalDivider()
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
            text = { M3Text(stringResource(R.string.usb_refresh_list)) },
            onClick = {
                UsbHost.refresh(context)
                onDismiss()
            },
        )
    }
}
