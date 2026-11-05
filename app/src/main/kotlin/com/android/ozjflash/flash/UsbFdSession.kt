package com.android.ozjflash.flash

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.system.Os

/**
 * 页面级常驻 USB fd 会话 —— 完整复刻 MOFLASH 的 `UsbHostManager`（object 单例）。
 *
 * ## 逆向依据（MOFLASH classes.dex 反汇编）
 *
 * - `UsbHostManager` 是 Kotlin `object`（字段 `a` = INSTANCE），
 *   持有 `c: UsbDeviceConnection`、`d: UsbDevice`、`e: fd`，跨页面/跨命令常驻。
 * - `open(Context, UsbDevice)`：`UsbManager.openDevice()` 后调用**公开 API**
 *   `UsbDeviceConnection.getFileDescriptor()`（0x4b7d08）拿原始 usbfs fd，
 *   把 connection 存进单例 `.c`，**不 dup、不反射 mFileDescriptor、不 claimInterface**。
 * - `currentFd()`（0x4b7ffc）：从单例 `.c` 现取 `getFileDescriptor()`，
 *   只要 connection 常驻，fd 一直有效。
 * - `close(Z)`（0x4b8894）：参数为 true 时**只关 connection、保留 fd 记录**，
 *   对应日志「usbhost fd 已保留(释放后可直接重连，无需插拔)」。
 * - 页面组合期 `DisposableEffect` 注册 `registerPermissionReceiver`，
 *   授权回调里立即 `open()` —— 这就是「打开页面就自动访问连接 USB」。
 *
 * ## 为什么之前连不上（根因）
 *
 * 旧实现反射隐藏字段 `mFileDescriptor` + `ParcelFileDescriptor.dup()`：
 * Android 9+ 隐藏 API 限制下反射常被拦，fd 取不到（=-1）或 dup 副本被 GC 关掉；
 * 且每条命令都 `openDevice` 一次、用完 `conn.close()`，fd 生命周期与
 * fdpass 常驻服务错位。这里全部改为「页面级 open 一次、单例常驻原始 fd」。
 *
 * ## 不 claim interface
 *
 * usbfs 的 claim 跟着文件句柄走：App 侧 claim 会让子进程 fastboot/adb 再 claim
 * 得到 EBUSY。魔改二进制内部自带 `detach_kernel_driver_and_claim`，由它自己 claim。
 */
object UsbFdSession {

    @Volatile
    private var connection: UsbDeviceConnection? = null

    @Volatile
    private var device: UsbDevice? = null

    @Volatile
    private var fd: Int = -1

    /** 当前常驻 fd；-1 表示尚未建立会话 */
    fun currentFd(): Int = fd

    fun isOpen(): Boolean = fd > 0 && connection != null

    fun currentDevice(): UsbDevice? = device

    /** 打开页面 / 执行命令前调用：确保有一个常驻、已授权的 usbfs fd */
    sealed interface Result {
        data class Ready(val fd: Int, val device: UsbDevice, val reused: Boolean) : Result
        data class NeedPermission(val device: UsbDevice) : Result
        data class NoDevice(val bus: List<String>) : Result
        data class OpenFailed(val device: UsbDevice) : Result
    }

    @Synchronized
    fun ensure(context: Context, tool: String): Result {
        // 已建立会话：先校验常驻 fd 仍指向 usbfs。设备拔插 / reboot 之后旧句柄会失效，
        // 若仍按「复用」直接返回，命令就会拿到一个已关闭的 fd —— 这正是之前
        // 「偶尔连上、设备一重启就又连不上」的隐性来源。
        if (isOpen()) {
            val link = runCatching { Os.readlink("/proc/self/fd/$fd") }.getOrNull()
            if (link != null && link.startsWith("/dev/bus/usb")) {
                return Result.Ready(fd, device!!, true)
            }
            runCatching { connection?.close() }
            connection = null
            fd = -1
        }

        val target = UsbHost.pickForTool(context, tool)
            ?: return Result.NoDevice(UsbHost.describeAll(context))

        val usb = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return Result.OpenFailed(target)
        if (!usb.hasPermission(target)) {
            UsbHost.requestPermission(context, target)
            return Result.NeedPermission(target)
        }

        // 清理可能残留的旧连接
        runCatching { connection?.close() }
        val conn = usb.openDevice(target) ?: return Result.OpenFailed(target)
        val rawFd = conn.getFileDescriptor() // 公开 API，原始 usbfs fd，无需反射/dup
        if (rawFd <= 0) {
            runCatching { conn.close() }
            return Result.OpenFailed(target)
        }
        connection = conn
        device = target
        fd = rawFd
        UsbHost.setPreferred(context, target)
        return Result.Ready(rawFd, target, false)
    }

    /**
     * 释放会话。keepFd=true 时对齐 MOFLASH `close(true)`：
     * 关闭 connection（子进程已退出），但保留 preferred device，便于下次静默重连。
     * 一般页面销毁不必调用 —— fd 常驻正是免 root 能连续执行多条命令的前提。
     */
    @Synchronized
    fun release(keepFd: Boolean = true) {
        runCatching { connection?.close() }
        connection = null
        if (!keepFd) {
            device = null
            fd = -1
        }
    }
}
