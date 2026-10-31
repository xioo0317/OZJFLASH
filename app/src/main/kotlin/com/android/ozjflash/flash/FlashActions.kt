package com.android.ozjflash.flash

/**
 * 刷写快捷指令：生成在「内置终端」中直接执行的 shell 命令。
 * 这些命令调用的 fastboot / adb 等工具已由 NativeLibs 释放到终端 PATH，
 * 因此无需设备 root 即可在设备进入 fastboot / bootloader 模式后下发。
 */
object FlashActions {
    fun rebootBootloader() = "reboot bootloader"
    fun fastbootDevices() = "fastboot devices"
    fun getvarAll() = "fastboot getvar all"
    fun flashPartition(partition: String, image: String) = "fastboot flash $partition $image"
    fun erasePartition(partition: String) = "fastboot erase $partition"
    fun rebootFastboot() = "fastboot reboot-bootloader"
    fun adbDevices() = "adb devices"
}
