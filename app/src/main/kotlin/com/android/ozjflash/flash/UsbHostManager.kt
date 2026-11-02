package com.android.ozjflash.flash

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * USB Host 通信管理（免 root 刷写的物理基础）。
 * 对应 APK 中的 com.Android.MoFlash.data.UsbHostManager：
 * 枚举/打开设备、请求 USB 权限，把 fd 交给底层协议库（fastboot / EDL / MTK）。
 */
class UsbHostManager(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun listDevices(): List<UsbDevice> = usbManager.deviceList.values.toList()
    fun findEdlDevice(): UsbDevice? = listDevices().firstOrNull { it.vendorId == 0x05c6 } // Qualcomm EDL (9008)
    fun findFastbootDevice(): UsbDevice? =
        listDevices().firstOrNull { it.vendorId == 0x18d1 || it.vendorId == 0x0fce || it.vendorId == 0x05c6 }
    fun hasPermission(d: UsbDevice): Boolean = usbManager.hasPermission(d)

    /** 弹出系统「允许 OZJFLASH 访问该 USB 设备」授权框，结果由 [UsbHost.receiver] 回收 */
    fun requestPermission(d: UsbDevice) = UsbHost.requestPermission(context, d)

    fun open(d: UsbDevice) = usbManager.openDevice(d)
}

/** 一个已连接的 USB 设备，以及它当前的授权状态 */
data class UsbDeviceInfo(
    val device: UsbDevice,
    val granted: Boolean,
) {
    val key: String get() = device.deviceName

    /** 形如 18D1:4EE0 的 VID:PID，方便对照 fastboot / EDL 端口 */
    val vidPid: String
        get() = "%04X:%04X".format(device.vendorId, device.productId)

    /** 优先用产品名 / 厂商名拼接，没有就退回 VID:PID */
    val label: String
        get() {
            val product = device.productName?.takeIf { it.isNotBlank() }
            val manufacturer = device.manufacturerName?.takeIf { it.isNotBlank() }
            val name = when {
                product != null && manufacturer != null -> "$manufacturer $product"
                product != null -> product
                manufacturer != null -> manufacturer
                else -> null
            }
            return if (name != null) "$name ($vidPid)" else vidPid
        }
}

/** 一次 USB 授权的结果，用于页面提示 */
data class UsbPermissionResult(
    val deviceKey: String?,
    val granted: Boolean,
)

/**
 * USB Host 全局状态与授权回调宿主。
 *
 * Android 的 USB 授权是异步的：调用 [requestPermission] 后系统弹框，
 * 结果通过广播回传。这里统一持有设备列表与授权结果，供 Compose 页面直接观察。
 */
object UsbHost {

    const val ACTION_USB_PERMISSION = "com.android.ozjflash.USB_PERMISSION"

    private val _devices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val devices: StateFlow<List<UsbDeviceInfo>> = _devices.asStateFlow()

    private val _lastResult = MutableStateFlow<UsbPermissionResult?>(null)
    val lastResult: StateFlow<UsbPermissionResult?> = _lastResult.asStateFlow()

    /** 重新枚举 USB 总线，刷新设备列表与授权状态 */
    fun refresh(context: Context) {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        _devices.value = manager.deviceList.values
            .map { UsbDeviceInfo(it, manager.hasPermission(it)) }
            .sortedWith(compareBy({ !it.granted }, { it.label }))
    }

    /** 请求访问某个 USB 设备；已授权则直接刷新状态，不再打扰用户 */
    fun requestPermission(context: Context, device: UsbDevice) {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        if (manager.hasPermission(device)) {
            _lastResult.value = UsbPermissionResult(device.deviceName, true)
            refresh(context)
            return
        }
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            device.deviceId, // 每个设备一个独立 requestCode，避免互相覆盖
            intent,
            flags,
        )
        manager.requestPermission(device, pendingIntent)
    }

    fun consumeResult() {
        _lastResult.value = null
    }

    val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == ACTION_USB_PERMISSION) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
                }
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                _lastResult.value = UsbPermissionResult(device?.deviceName, granted)
            }
            // 授权结果、设备插拔都会改变可见状态，统一重扫一次
            refresh(context)
        }
    }

    fun intentFilter(): IntentFilter = IntentFilter().apply {
        addAction(ACTION_USB_PERMISSION)
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
    }
}
