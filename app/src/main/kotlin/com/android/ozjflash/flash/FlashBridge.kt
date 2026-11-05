package com.android.ozjflash.flash

import android.content.Context
import com.Android.MoFlash.data.NativeCmd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 刷写桥接层：把「连接模式判定 / USB fd 抽取 / 原生桥(JNI) / 内置二进制命令」统一收口。
 *
 * 两条执行路径（对标 MOFLASH 的 root / usbhost 切换）：
 *  - usbhost（免 root，默认）：用 UsbHostManager 打开设备拿到 fd，直接在应用进程内
 *    调 NativeCmd 的 JNI 方法（libnativecmd.so 已 load），由该 fd 与设备通信；
 *  - root：直接把命令交给内置二进制（./fastboot 等）执行，依赖环境本身提供访问能力。
 *
 * 所有原生调用都包了 try/catch：一旦 JNI 不可用（如 .so 缺失或签名不匹配），
 * 自动回退到终端里的 shell 命令，保证功能不中断。
 */
object FlashBridge {

    /** false = 免 root 走 usbhost/JNI；true = 直接调用内置二进制 */
    private val _rootMode = MutableStateFlow(false)
    val rootMode: StateFlow<Boolean> = _rootMode.asStateFlow()
    fun setRootMode(v: Boolean) { _rootMode.value = v }

    /** 选目标设备并打开，返回其在应用进程内的 fd；失败返回 -1 */
    fun openTargetFd(context: Context): Int {
        val dev = UsbHost.pickTarget(context) ?: return -1
        return UsbHostManager(context).openWithFd(dev)?.second ?: -1
    }

    // —— 原生桥（进程内 JNI，免 root）——
    /** 选目标设备→打开→抽取 fd→执行 block→关闭连接，统一托管连接生命周期 */
    inline fun <R> withTargetFd(context: Context, block: (fd: Int) -> R): R? {
        val dev = UsbHost.pickTarget(context) ?: return null
        val mgr = UsbHostManager(context)
        val pair = mgr.openWithFd(dev) ?: return null
        val (conn, fd) = pair
        return try {
            block(fd)
        } finally {
            runCatching { conn.close() }
            runCatching { releaseFd(fd) }
        }
    }

    /** 原生桥方法均为 fire-and-forget（Unit 返回），统一以 Boolean 表示成败 */
    fun nativeFlash(fd: Int, partition: String, image: String): Boolean =
        runCatching { NativeCmd().nFlash(fd, partition, image); true }.getOrElse { false }

    fun nativeFlashPart(fd: Int, partition: String, image: String): Boolean =
        runCatching { NativeCmd().nFlashPart(fd, partition, image); true }.getOrElse { false }

    fun nativeGetvarAll(fd: Int): Boolean =
        runCatching { NativeCmd().nGetvarAll(fd); true }.getOrElse { false }

    fun nativeRebootMode(fd: Int, mode: String): Boolean =
        runCatching { NativeCmd().nRebootMode(fd, mode); true }.getOrElse { false }

    fun nativeErase(fd: Int, partition: String): Boolean =
        runCatching { NativeCmd().nErasePart(fd, partition); true }.getOrElse { false }

    /** 免 root 直连刷写：打开 USB→取 fd→进程内 JNI 调用→关闭 */
    fun flashViaNative(context: Context, partition: String, image: String): Boolean =
        withTargetFd(context) { NativeCmd().nFlash(it, partition, image); true } ?: false

    fun getvarViaNative(context: Context): Boolean =
        withTargetFd(context) { NativeCmd().nGetvarAll(it); true } ?: false

    fun rebootViaNative(context: Context, mode: String): Boolean =
        withTargetFd(context) { NativeCmd().nRebootMode(it, mode); true } ?: false

    fun eraseViaNative(context: Context, partition: String): Boolean =
        withTargetFd(context) { NativeCmd().nErasePart(it, partition); true } ?: false

    // —— 终端 shell 命令（PATH 已注入内置二进制，作为回退/补充）——
    fun cmdFlash(partition: String, image: String) = "fastboot flash $partition $image"
    fun cmdGetvarAll() = "fastboot getvar all"
    fun cmdRebootBootloader() = "reboot bootloader"
    fun cmdRebootFastboot() = "fastboot reboot-bootloader"
    fun cmdErase(partition: String) = "fastboot erase $partition"

    /** 高通 EDL：用内置 qdl 二进制 + loader 文件刷写（loader 由 7z 解出的双广/loader 包提供） */
    fun cmdQdl(loader: String) = "./qdl $loader"

    /** MTK：scatter 文本 + DA/Preloader，经 SP Flash 类流程（此处以 qdl/mtkcmd 二进制承接） */
    fun cmdMtk(scatter: String, da: String) = "./mtkcmd --scatter $scatter --da $da"

    /** OTA：payload-dumper-go 解出 payload.bin 中的各分区镜像 */
    fun cmdPayloadDumper(payload: String, outDir: String) = "payload-dumper-go -o $outDir $payload"

    /** 7z 解包 loader / 线刷包（lib7zz.so 即 7z 本体） */
    fun cmdSevenZipExtract(archive: String, outDir: String) = "7z x $archive -o$outDir"

    /**
     * root 模式：用 su 以绝对路径直接执行 fastboot。
     * su 环境的 PATH 不含应用的 bin 目录，所以必须传 binPath/fastboot 完整路径，
     * 而非裸 `fastboot`（否则 su -c 找不到命令）。
     */
    fun cmdSuFastboot(binPath: String, args: String) = "su -c \"$binPath/fastboot $args\""
}
