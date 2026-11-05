package com.android.ozjflash.flash

import android.hardware.usb.UsbDevice
import android.system.Os

/**
 * 免 root「fd 直通」链路的逐段自检。
 *
 * 逆向 MOFLASH 得到的真实约定（libfastboot.so / libadb.so 内）：
 *   1. `getenv("MOMIUI_USB_SOCK")` —— 值必须以 `@` 开头，是 Unix 抽象 socket 名；
 *   2. 若该变量无效，再退化到 `getenv("MOMIUI_USB_FD")` —— 进程内直接给 fd 号；
 *   3. socket 路径：connect 到 `@` 之后的名字，成功后发 1 字节请求，
 *      再用 recvmsg + SCM_RIGHTS 收取已授权的 USB fd，最后走 `/proc/self/fd/<n>`。
 *
 * 注意：**只设 MOMIUI_USB_FD 是不够的**，魔改版优先读 MOMIUI_USB_SOCK。
 */
object UsbFdTracer {

    fun device(d: UsbDevice, mode: UsbMode, granted: Boolean): String {
        val name = d.productName?.takeIf { it.isNotBlank() } ?: d.deviceName
        return "[诊断] 设备 $name (%04X:%04X) 模式=%s 授权=%s"
            .format(d.vendorId, d.productId, mode.label, if (granted) "是" else "否")
    }

    /** fastboot 接口应为 FF/42/03，ADB 为 FF/42/01 */
    fun interfaces(d: UsbDevice): String {
        if (d.interfaceCount == 0) return "[诊断] 接口 <无>"
        val items = (0 until d.interfaceCount).joinToString("  ") { i ->
            val itf = d.getInterface(i)
            "#$i %02X/%02X/%02X".format(
                itf.interfaceClass,
                itf.interfaceSubclass,
                itf.interfaceProtocol,
            )
        }
        return "[诊断] 接口 $items"
    }

    /** /proc/self/fd/N 若指向 /dev/bus/usb/... 才说明 openDevice 真的拿到了 usbfs 句柄 */
    fun fd(fd: Int): String {
        val target = runCatching { Os.readlink("/proc/self/fd/$fd") }.getOrNull()
            ?: return "[诊断] fd=$fd （无效或已被关闭）"
        val ok = target.startsWith("/dev/bus/usb")
        return "[诊断] fd=$fd → $target ${if (ok) "✔ usbfs" else "✘ 非 usbfs（传给工具也无法通信）"}"
    }

    fun socket(name: String): String {
        val ok = name.startsWith("@")
        return "[诊断] socket=$name ${if (ok) "✔ 抽象命名空间" else "✘ 缺少 @ 前缀"}"
    }
}
