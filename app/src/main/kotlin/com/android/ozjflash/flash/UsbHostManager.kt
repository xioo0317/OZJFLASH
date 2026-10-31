package com.android.ozjflash.flash

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

/**
 * USB Host 通信管理（免 root 刷写的物理基础）。
 * 对应 APK 中的 com.Android.MoFlash.data.UsbHostManager：
 * 枚举/打开设备、请求 USB 权限，把 fd 交给底层协议库（fastboot / EDL / MTK）。
 *
 * 说明：完整 fd 传递（FdPass / libfdpass）与授权回调需结合 Activity 的
 * PendingIntent 实现；此处给出可工作的骨架，UI 层负责请求权限后调用 open()。
 */
class UsbHostManager(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun listDevices(): List<UsbDevice> = usbManager.deviceList.values.toList()
    fun findEdlDevice(): UsbDevice? = listDevices().firstOrNull { it.vendorId == 0x05c6 } // Qualcomm EDL (9008)
    fun findFastbootDevice(): UsbDevice? =
        listDevices().firstOrNull { it.vendorId == 0x18d1 || it.vendorId == 0x0fce || it.vendorId == 0x05c6 }
    fun hasPermission(d: UsbDevice): Boolean = usbManager.hasPermission(d)
    fun requestPermission(d: UsbDevice) { /* 通过 PendingIntent 请求，回调在 Activity 处理 */ }
    fun open(d: UsbDevice) = usbManager.openDevice(d)
}
