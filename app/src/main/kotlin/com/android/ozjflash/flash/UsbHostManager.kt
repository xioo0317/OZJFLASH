package com.android.ozjflash.flash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.app.PendingIntent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * USB Host 通信管理（免 root 刷写的物理基础）。
 *
 * 无 root 下，Android 不允许直接访问 /dev/bus/usb，而是通过 UsbManager 枚举并
 * 在用户授权后 openDevice() 拿到 UsbDeviceConnection。本类负责：
 *   1. 枚举 / 打开设备、请求 USB 权限；
 *   2. classifyDevice() 仅依据 VID/PID 与接口描述符推断「连接模式」
 *      （EDL / Fastboot / ADB / MTK BROM …），无需任何 root 能力；
 *   3. currentFd() 把已打开连接的 fd 抽出来，交给底层协议库（fastboot / EDL / MTK）
 *      经 FdPass 在进程间传递（免 root 直连的关键）。
 *
 * 该设计对标 MOFLASH 的 com.Android.MoFlash.data.UsbHostManager。
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

    fun open(d: UsbDevice): UsbDeviceConnection? = usbManager.openDevice(d)

    /**
     * 打开设备并抽取其文件描述符（供免 root 的底层协议库使用）。
     * 返回 null 表示未授权或打开失败。调用方负责在 fd 用完后关闭 connection。
     */
    fun openWithFd(d: UsbDevice): Pair<UsbDeviceConnection, Int>? {
        if (!usbManager.hasPermission(d)) return null
        val conn = usbManager.openDevice(d) ?: return null
        val fd = currentFd(conn)
        if (fd < 0) { conn.close(); return null }
        return conn to fd
    }
}

/** 一次 USB 授权的结果，用于页面提示 */
data class UsbPermissionResult(
    val deviceKey: String?,
    val granted: Boolean,
)

/**
 * USB 连接模式（无 root，仅靠 VID/PID + 接口描述符判断）。
 * 与 MOFLASH 的 classifyDevice 判定规则对齐。
 */
enum class UsbMode(val label: String) {
    EDL("EDL"),
    DLOAD("DLoad"),
    FASTBOOT("Fastboot"),
    ADB("ADB"),
    MTK_BROM("MTK BROM/Preloader"),
    QUALCOMM("Qualcomm"),
    XIAOMI("Xiaomi"),
    OPPO("OPPO/realme"),
    ONEPLUS("OnePlus"),
    ANDROID_BRAND("Android"),
    UNKNOWN("未知");

    /** 是否为可用于刷写的设备模式 */
    val isFlashable: Boolean
        get() = this in setOf(EDL, DLOAD, FASTBOOT, MTK_BROM)
}

/** Vendor / Product ID 常量（与 classifyDevice 共用） */
private const val VID_QUALCOMM = 0x05C6
private const val VID_GOOGLE = 0x18D1
private const val VID_MTK = 0x0E8D
private const val VID_OPPO = 0x22C9
private const val VID_XIAOMI = 0x2717
private const val VID_ONEPLUS = 0x2A70
private const val VID_ANDROID = 0x1234

/** 已知的 MediaTek Preloader / BROM PID 集合（MOFLASH 中以 Set 维护，这里给出常用值） */
private val MTK_BROM_PIDS = setOf(
    0x0003, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007,
    0x2008, 0x2009, 0x200A, 0x200B, 0x200C, 0x200D, 0x200E, 0x200F,
    0x2010, 0x2011, 0x2012, 0x2013, 0x2014, 0x2015,
)

/**
 * 依据 VID/PID 与接口描述符推断 USB 设备的连接模式（无 root）。
 * 规则：
 *  - 高通(0x05C6)：9006/9008/900C/900E/901D/9091=EDL、9018=DLoad、其余=Qualcomm；
 *  - Google(0x18D1)：PID∈[0xD01,0xD1F]=Fastboot、0x2A00=ADB；
 *  - 接口兜底：class 0xFF / subclass 0x42 → protocol 3=Fastboot、1=ADB；
 *  - MTK(0x0E8D)：0x201C=Fastboot、在已知集合=BROM、其余归 BROM；
 *  - OPPO/Xiaomi/OnePlus/通用 Android 仅打品牌标签。
 */
fun classifyDevice(device: UsbDevice): UsbMode {
    val vid = device.vendorId
    val pid = device.productId

    if (vid == VID_QUALCOMM) {
        return when (pid) {
            0x9006, 0x9008, 0x900C, 0x900E, 0x901D, 0x9091 -> UsbMode.EDL
            0x9018 -> UsbMode.DLOAD
            else -> UsbMode.QUALCOMM
        }
    }
    if (vid == VID_GOOGLE) {
        if (pid in 0x0D01..0x0D1F) return UsbMode.FASTBOOT
        if (pid == 0x2A00) return UsbMode.ADB
    }
    // 接口描述符兜底（ADB/Fastboot 在 Android 上的标准接口：FF/42/03 与 FF/42/01）
    for (i in 0 until device.interfaceCount) {
        val itf = device.getInterface(i)
        if (itf.interfaceClass == 0xFF && itf.interfaceSubclass == 0x42) {
            when (itf.interfaceProtocol) {
                3 -> return UsbMode.FASTBOOT
                1 -> return UsbMode.ADB
            }
        }
    }
    if (vid == VID_MTK) {
        if (pid == 0x201C) return UsbMode.FASTBOOT
        if (MTK_BROM_PIDS.contains(pid)) return UsbMode.MTK_BROM
        return UsbMode.MTK_BROM
    }
    return when (vid) {
        VID_OPPO -> UsbMode.OPPO
        VID_XIAOMI -> UsbMode.XIAOMI
        VID_ONEPLUS -> UsbMode.ONEPLUS
        VID_ANDROID -> UsbMode.ANDROID_BRAND
        else -> UsbMode.UNKNOWN
    }
}

/** 从 UsbDeviceConnection 反射取出文件描述符（不消费原始 fd），供进程内/进程间传递 */
fun currentFd(connection: UsbDeviceConnection): Int {
    val pfd = reflectPfd(connection) ?: return -1
    return try {
        // 复制出一份独立的 fd：底层协议库（fastboot/EDL/MTK）拿到的是 dup 出来的副本，
        // 原始 UsbDeviceConnection 在 FlashBridge.withTargetFd 的 finally 中关闭时不会误伤它。
        ParcelFileDescriptor.dup(pfd.fileDescriptor).fd
    } catch (_: Throwable) {
        pfd.fd
    }
}

private fun reflectPfd(c: UsbDeviceConnection): ParcelFileDescriptor? {
    return try {
        val field = c.javaClass.getDeclaredField("mFileDescriptor").apply { isAccessible = true }
        field.get(c) as? ParcelFileDescriptor
    } catch (_: Throwable) {
        null
    }
}

/** 一个已连接的 USB 设备，以及它当前的授权状态与连接模式 */
data class UsbDeviceInfo(
    val device: UsbDevice,
    val granted: Boolean,
    val mode: UsbMode,
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

/** USB Host 全局状态与授权回调宿主 */
object UsbHost {

    const val ACTION_USB_PERMISSION = "com.android.ozjflash.USB_PERMISSION"
    private const val PREF_NAME = "ozj_usb_prefs"
    private const val KEY_PREFERRED = "preferred_device"

    private val _devices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val devices: StateFlow<List<UsbDeviceInfo>> = _devices.asStateFlow()

    private val _lastResult = MutableStateFlow<UsbPermissionResult?>(null)
    val lastResult: StateFlow<UsbPermissionResult?> = _lastResult.asStateFlow()

    /** 重新枚举 USB 总线，刷新设备列表、授权状态与连接模式 */
    fun refresh(context: Context) {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        _devices.value = manager.deviceList.values
            .map { UsbDeviceInfo(it, manager.hasPermission(it), classifyDevice(it)) }
            .sortedWith(compareBy({ !it.granted }, { it.mode.ordinal }, { it.label }))
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

    /** 记忆首选设备（下次自动路由到它） */
    fun setPreferred(context: Context, device: UsbDevice) {
        prefs(context).edit { putString(KEY_PREFERRED, device.deviceName) }
    }

    fun preferredKey(context: Context): String? {
        val key = prefs(context).getString(KEY_PREFERRED, null)
        return if (key.isNullOrEmpty()) null else key
    }

    /**
     * 命令行专用：按工具挑设备。
     *
     * 不能复用 pickTarget —— 后者把 EDL / BROM 也算「可刷写」，
     * 一旦总线上一台下载模式设备，fastboot 就会去开一个根本不是 fastboot 的接口。
     * fastboot 只认 FF/42/03，adb 只认 FF/42/01。
     */
    fun pickForTool(context: Context, tool: String): UsbDevice? {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return null
        val want = if (tool == "adb") UsbMode.ADB else UsbMode.FASTBOOT
        val candidates = manager.deviceList.values.filter { classifyDevice(it) == want }
        if (candidates.isEmpty()) return null
        val preferred = preferredKey(context)
        return candidates.firstOrNull { it.deviceName == preferred }
            ?: candidates.firstOrNull { manager.hasPermission(it) }
            ?: candidates.firstOrNull()
    }

    /** 无目标设备时，把当前总线上有什么打出来，便于判断是不是没进对应模式 */
    fun describeAll(context: Context): List<String> {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        return manager.deviceList.values.map { d ->
            "%04X:%04X %s".format(d.vendorId, d.productId, classifyDevice(d).label)
        }
    }

    /** 在已枚举设备中挑出首选（记住的）或第一个已授权的可刷写设备 */
    fun pickTarget(context: Context): UsbDevice? {
        val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return null
        val list = manager.deviceList.values.toList()
        val preferred = preferredKey(context)
        if (preferred != null) {
            list.firstOrNull { it.deviceName == preferred }?.let { return it }
        }
        return list.firstOrNull { manager.hasPermission(it) && classifyDevice(it).isFlashable }
            ?: list.firstOrNull { classifyDevice(it).isFlashable }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

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
