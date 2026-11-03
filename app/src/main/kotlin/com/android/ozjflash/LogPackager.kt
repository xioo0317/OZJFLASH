package com.android.ozjflash

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志打包：照搬 KernelSU 的「导出日志」思路（收集多来源日志 → 打成一个压缩包 → 分享）。
 *
 * 与 KernelSU 的差异：KSU 是 root 应用，能直接抓 dmesg / 内核模块日志；
 * OZJFLASH 免 root，因此改为收集应用私有目录下的所有 *.log，并尽力追加一份
 * logcat 快照与设备信息，最后打包成 zip 供 FileProvider 分享。
 */
object LogPackager {

    /** 汇总所有日志文件 + 设备信息 + logcat 成一个 zip，返回 zip 文件；无日志时返回 null */
    fun packageLogs(context: Context): File? {
        val app = context.applicationContext as Application
        val logFiles = CrashLogger.allLogFiles(app)
        if (logFiles.isEmpty()) return null

        val outDir = File(context.cacheDir, "logs").also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(outDir, "OZJFLASH_logs_$stamp.zip")

        val ok = runCatching {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                logFiles.forEach { addFile(zos, it, it.name) }

                zos.putNextEntry(ZipEntry("device_info.txt"))
                zos.write(buildDeviceInfo(context).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 尽力而为：非 root 应用读 logcat 可能为空，仅在有内容时写入
                runCatching {
                    val proc = Runtime.getRuntime().exec("logcat -d -v time")
                    val logcat = proc.inputStream.bufferedReader().use(BufferedReader::readText)
                    proc.waitFor()
                    if (logcat.isNotBlank()) {
                        zos.putNextEntry(ZipEntry("logcat.txt"))
                        zos.write(logcat.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
            }
        }.isSuccess

        return if (ok && zipFile.exists() && zipFile.length() > 0) zipFile else null
    }

    private fun addFile(zos: ZipOutputStream, file: File, entryName: String) {
        runCatching {
            FileInputStream(file).use { fis ->
                zos.putNextEntry(ZipEntry(entryName))
                fis.copyTo(zos)
                zos.closeEntry()
            }
        }
    }

    private fun buildDeviceInfo(context: Context): String = buildString {
        appendLine("=== OZJFLASH device info ===")
        appendLine("time   : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        appendLine("device : ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("sdk    : ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        appendLine("abi    : ${Build.SUPPORTED_ABIS.joinToString()}")
        runCatching {
            @Suppress("DEPRECATION")
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION") pi.versionCode.toString()
            }
            appendLine("app    : ${pi.versionName} ($code)")
        }
    }
}
