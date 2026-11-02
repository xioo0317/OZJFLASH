package com.android.ozjflash.ui.component.usb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.UsbDeviceInfo
import com.android.ozjflash.flash.UsbHost
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.material.ExpressiveDialog
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * USB Host 授权卡片：列出当前 USB 总线上的设备，未授权的点一下先弹应用内确认框，
 * 再交给系统弹「允许 OZJFLASH 访问该 USB 设备」授权框。
 * 授权成功后 fastboot / adb / EDL 工具才拿得到设备句柄。
 */
@Composable
fun UsbHostCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val devices by UsbHost.devices.collectAsStateWithLifecycle()
    val result by UsbHost.lastResult.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<UsbDeviceInfo?>(null) }
    val uiMode = LocalUiMode.current

    LaunchedEffect(Unit) { UsbHost.refresh(context) }

    when (uiMode) {
        UiMode.Miuix -> UsbHostCardMiuix(
            devices = devices,
            denied = result?.granted == false,
            onRequest = { pending = it },
            modifier = modifier,
        )

        UiMode.Material -> UsbHostCardMaterial(
            devices = devices,
            denied = result?.granted == false,
            onRequest = { pending = it },
            modifier = modifier,
        )
    }

    pending?.let { info ->
        when (uiMode) {
            UiMode.Miuix -> UsbPermissionDialogMiuix(
                info = info,
                onConfirm = { UsbHost.requestPermission(context, info.device) },
                onDismiss = { pending = null },
            )

            UiMode.Material -> UsbPermissionDialogMaterial(
                info = info,
                onConfirm = { UsbHost.requestPermission(context, info.device) },
                onDismiss = { pending = null },
            )
        }
    }
}

@Composable
private fun UsbHostCardMiuix(
    devices: List<UsbDeviceInfo>,
    denied: Boolean,
    onRequest: (UsbDeviceInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        SmallTitle(
            text = stringResource(R.string.usb_devices),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (devices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.usb_none),
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariantSummary
                    )
                } else {
                    devices.forEach { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = info.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = info.vidPid,
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariantSummary
                                )
                            }
                            if (info.granted) {
                                Text(
                                    text = stringResource(R.string.usb_granted),
                                    fontSize = 12.sp,
                                    color = colorScheme.primary
                                )
                            } else {
                                TextButton(
                                    text = stringResource(R.string.usb_request),
                                    onClick = { onRequest(info) },
                                    colors = ButtonDefaults.textButtonColorsPrimary()
                                )
                            }
                        }
                    }
                }
                if (denied) {
                    Text(
                        text = stringResource(R.string.usb_denied),
                        fontSize = 12.sp,
                        color = colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.usb_hint),
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun UsbHostCardMaterial(
    devices: List<UsbDeviceInfo>,
    denied: Boolean,
    onRequest: (UsbDeviceInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        M3Text(
            text = stringResource(R.string.usb_devices),
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (devices.isEmpty()) {
                    M3Text(
                        text = stringResource(R.string.usb_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    devices.forEach { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                M3Text(text = info.label, style = MaterialTheme.typography.bodyMedium)
                                M3Text(
                                    text = info.vidPid,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            if (info.granted) {
                                M3Text(
                                    text = stringResource(R.string.usb_granted),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.primary
                                )
                            } else {
                                M3TextButton(onClick = { onRequest(info) }) {
                                    M3Text(stringResource(R.string.usb_request))
                                }
                            }
                        }
                    }
                }
                if (denied) {
                    M3Text(
                        text = stringResource(R.string.usb_denied),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                M3Text(
                    text = stringResource(R.string.usb_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun UsbPermissionDialogMiuix(
    info: UsbDeviceInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        title = stringResource(R.string.usb_grant_title),
        onDismissRequest = {
            UsbHost.consumeResult()
            onDismiss()
        },
        content = {
            val dismissState = LocalDismissState.current
            Column {
                Text(text = stringResource(R.string.usb_grant_message, info.label))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = {
                            UsbHost.consumeResult()
                            onDismiss()
                            dismissState?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.usb_allow),
                        onClick = {
                            onConfirm()
                            onDismiss()
                            dismissState?.invoke()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

@Composable
private fun UsbPermissionDialogMaterial(
    info: UsbDeviceInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ExpressiveDialog(
        onDismissRequest = {
            UsbHost.consumeResult()
            onDismiss()
        },
        title = { M3Text(stringResource(R.string.usb_grant_title)) },
        text = { M3Text(stringResource(R.string.usb_grant_message, info.label)) },
        confirmButton = {
            M3TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { M3Text(stringResource(R.string.usb_allow)) }
        },
        dismissButton = {
            M3TextButton(onClick = {
                UsbHost.consumeResult()
                onDismiss()
            }) { M3Text(stringResource(android.R.string.cancel)) }
        },
    )
}
