package com.android.ozjflash.ui.component.usb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.flash.UsbDeviceInfo
import com.android.ozjflash.flash.UsbHostManager
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.material.ExpressiveDialog
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 一行「名称 — 值」，值右对齐 */
private data class UsbInfoRow(val label: String, val value: String)

/**
 * USB 设备信息弹窗：点 USB 胶囊下拉里的设备条目后弹出，列出 USB 总线上该设备的全部可见信息。
 *
 * 数据全部来自 [android.hardware.usb.UsbDevice]（无需 root）：
 * 设备类/子类/协议、接口数、设备节点路径、序列号都在 UsbDevice 描述符里；
 * fd 与「已打开」需要授权后临时 openDevice() 取一次再立即关闭，取不到就显示未打开。
 */
@Composable
fun UsbDeviceInfoDialog(info: UsbDeviceInfo, onDismiss: () -> Unit) {
    val rows = usbInfoRows(info)
    when (LocalUiMode.current) {
        UiMode.Miuix -> UsbDeviceInfoDialogMiuix(rows, onDismiss)
        UiMode.Material -> UsbDeviceInfoDialogMaterial(rows, onDismiss)
    }
}

/**
 * 组装设备信息行。序列号在部分 ROM 上需要授权才可读（未授权抛 SecurityException），
 * 这里统一 runCatching 兜底，读不到就显示「—」而不是让弹窗崩掉。
 */
@Composable
private fun usbInfoRows(info: UsbDeviceInfo): List<UsbInfoRow> {
    val context = LocalContext.current
    val device = info.device
    val unknown = "—"

    // 已授权才尝试取一次 fd：openWithFd 内部会 openDevice()，这里取完立刻 close，
    // 不长期占用设备句柄，避免影响随后 fastboot / adb 的独占访问
    val fd = remember(info.key, info.granted) {
        if (!info.granted) {
            -1
        } else {
            runCatching {
                val manager = UsbHostManager(context)
                manager.openWithFd(device)?.let { (conn, fdValue) ->
                    conn.close()
                    fdValue
                } ?: -1
            }.getOrDefault(-1)
        }
    }

    val serial = remember(info.key) {
        runCatching { device.serialNumber }.getOrNull()?.takeIf { it.isNotBlank() } ?: unknown
    }
    val productName = remember(info.key) {
        runCatching { device.productName }.getOrNull()?.takeIf { it.isNotBlank() } ?: device.deviceName
    }
    val connectionState = if (info.granted) "device" else "unauthorized"

    return listOf(
        UsbInfoRow(stringResource(R.string.usb_info_type), info.mode.label),
        UsbInfoRow(stringResource(R.string.usb_info_vidpid), info.vidPid),
        UsbInfoRow(stringResource(R.string.usb_info_name), productName),
        UsbInfoRow(stringResource(R.string.usb_info_serial), serial),
        UsbInfoRow(stringResource(R.string.usb_info_path), device.deviceName),
        UsbInfoRow(stringResource(R.string.usb_info_interfaces), device.interfaceCount.toString()),
        UsbInfoRow(
            stringResource(R.string.usb_info_class),
            "${device.deviceClass} / ${device.deviceSubclass} / ${device.deviceProtocol}",
        ),
        UsbInfoRow(stringResource(R.string.usb_info_connection), "$serial $connectionState"),
        UsbInfoRow(stringResource(R.string.usb_info_fd), fd.toString()),
        UsbInfoRow(
            stringResource(R.string.usb_info_tool_state),
            if (fd >= 0) "Yes($serial $connectionState)" else "No",
        ),
        UsbInfoRow(
            stringResource(R.string.usb_info_opened),
            stringResource(if (fd >= 0) R.string.usb_info_yes else R.string.usb_info_no),
        ),
    )
}

@Composable
private fun UsbDeviceInfoDialogMiuix(rows: List<UsbInfoRow>, onDismiss: () -> Unit) {
    val colorScheme = MiuixTheme.colorScheme
    OverlayDialog(
        show = true,
        title = stringResource(R.string.usb_info_title),
        summary = stringResource(R.string.usb_info_summary),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MiuixText(
                        text = row.label,
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                    )
                    MiuixText(
                        text = row.value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
            MiuixTextButton(
                text = stringResource(R.string.usb_info_close),
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun UsbDeviceInfoDialogMaterial(rows: List<UsbInfoRow>, onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    ExpressiveDialog(
        onDismissRequest = onDismiss,
        title = { M3Text(stringResource(R.string.usb_info_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        M3Text(
                            text = row.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        M3Text(
                            text = row.value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            M3TextButton(onClick = onDismiss) { M3Text(stringResource(R.string.usb_info_close)) }
        },
    )
}
