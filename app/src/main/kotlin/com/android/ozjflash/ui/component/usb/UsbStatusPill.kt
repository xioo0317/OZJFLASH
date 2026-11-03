package com.android.ozjflash.ui.component.usb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.UsbHost

/**
 * 顶栏 USB 状态胶囊：照搬用户参考图——右侧一个圆角胶囊按钮，
 * 内含状态灰点 + 文案 + 上箭头；点击弹出下拉（设备列表 / 请求授权 / 刷新列表）。
 *
 * 把原本工具页里的整块 [UsbHostCard] 搬到了顶栏，页面更清爽。
 * [dark] 为 true 时按终端纯黑背景配色（浅字 + 半透明深底），否则跟随浅色主题。
 */
@Composable
fun UsbStatusPill(dark: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val devices by UsbHost.devices.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    val granted = devices.count { it.granted }
    val (dotColor, statusText) = when {
        devices.isEmpty() ->
            (if (dark) Color(0xFF9AA0A6) else Color(0xFF9E9E9E)) to
                stringResource(R.string.usb_status_none)
        granted > 0 ->
            Color(0xFF4CAF50) to stringResource(R.string.usb_status_connected, granted)
        else ->
            Color(0xFFFFB300) to stringResource(R.string.usb_status_connected, devices.size)
    }

    val pillBg = if (dark) Color(0xFF2A2A2A) else Color(0xFFE8EAED)
    val textColor = if (dark) Color(0xFFE6E6E6) else Color(0xFF1F1F1F)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(pillBg)
            .clickable { expanded = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(statusText, color = textColor, fontSize = 13.sp)
            Icon(
                Icons.Rounded.ArrowDropUp,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (devices.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.usb_menu_no_device)) },
                    onClick = { expanded = false },
                    enabled = false,
                )
            } else {
                devices.forEach { info ->
                    DropdownMenuItem(
                        text = { Text(info.label) },
                        trailingIcon = {
                            if (info.granted) {
                                Text(
                                    stringResource(R.string.usb_granted),
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                )
                            } else {
                                TextButton(onClick = {
                                    UsbHost.requestPermission(context, info.device)
                                    expanded = false
                                }) { Text(stringResource(R.string.usb_request)) }
                            }
                        },
                        onClick = {
                            if (!info.granted) UsbHost.requestPermission(context, info.device)
                            expanded = false
                        },
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                text = { Text(stringResource(R.string.usb_refresh_list)) },
                onClick = {
                    UsbHost.refresh(context)
                    expanded = false
                },
            )
        }
    }
}
