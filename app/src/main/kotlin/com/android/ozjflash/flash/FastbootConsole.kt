package com.android.ozjflash.flash

import android.content.Context
import android.os.ParcelFileDescriptor
import com.Android.MoFlash.data.FdPass
import java.io.File

/**
 * Fastboot 命令行执行器。
 *
 * 两条执行路径（对齐 MOFLASH 的免 root / root 切换）：
 *  - 免 root（默认）：用 UsbManager 打开设备拿到 fd，再通过 FdPass 的 Unix 抽象
 *    socket（SCM_RIGHTS）把 fd 交给内置 fastboot；该 fastboot 为魔改版，读取
 *    MOMIUI_USB_FD（形如 "@xxx"）连接 socket 收 fd，随后走 /proc/self/fd/<n>
 *    直接操作设备——完全绕开 /dev/bus/usb 的权限限制；
 *  - root：`su -c "<bin>/fastboot ..."` 直接执行。
 *
 * 每条命令都是独立短命进程，执行完即退出，不占用常驻 bash / PTY。
 */
object FastbootConsole {

    /** 规范化输入：去掉可选的 `fastboot` 前缀，并做带引号的分词 */
    fun normalize(raw: String): List<String> {
        val t = raw.trim()
        if (t.isEmpty()) return emptyList()
        val body = when {
            t == "fastboot" -> ""
            t.startsWith("fastboot ") -> t.removePrefix("fastboot").trim()
            else -> t
        }
        return if (body.isEmpty()) emptyList() else splitArgs(body)
    }

    /** 简易分词：支持单/双引号，避免带空格的路径被拆散 */
    private fun splitArgs(s: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var quote: Char? = null
        for (c in s) {
            when {
                quote != null -> if (c == quote) quote = null else cur.append(c)
                c == '\'' || c == '"' -> quote = c
                c.isWhitespace() -> if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() }
                else -> cur.append(c)
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }

    /**
     * 执行一条 fastboot 命令，逐行回调输出。
     *
     * @return 进程退出码；-1 表示空命令 / 二进制缺失 / 设备未授权 / 启动失败。
     */
    fun run(
        context: Context,
        raw: String,
        rootMode: Boolean,
        onLine: (String) -> Unit,
    ): Int {
        val args = normalize(raw)
        if (args.isEmpty()) return -1

        val binPath = File(context.filesDir, "bin").absolutePath
        val exe = File(binPath, "fastboot")
        if (!exe.exists()) {
            onLine("[错误] 内置 fastboot 不存在：${exe.absolutePath}")
            return -1
        }

        if (rootMode) {
            val command = listOf(
                "su", "-c",
                exe.absolutePath + " " + args.joinToString(" ") { quote(it) },
            )
            return exec(context, binPath, command, emptyMap(), onLine)
        }

        // —— 免 root：打开 USB 设备 → FdPass 传 fd → 魔改 fastboot 直连 ——
        val manager = UsbHostManager(context)
        val device = UsbHost.pickTarget(context)
        if (device == null) {
            onLine("[错误] 未发现 USB 设备，请先连接设备")
            return -1
        }
        if (!manager.hasPermission(device)) {
            manager.requestPermission(device)
            onLine("[提示] 尚未授权 USB 访问，已弹出系统授权框，请允许后重新执行")
            return -1
        }
        val pair = manager.openWithFd(device)
        if (pair == null) {
            onLine("[错误] 打开 USB 设备失败（无法取得 fd）")
            return -1
        }
        val (conn, fd) = pair

        // 每次执行用独立 socket 名，避免并发/残留互相干扰
        val socketName = "@OZJ_USB_FD_${System.nanoTime()}"
        val server = Thread {
            runCatching { FdPass().serveFdLoop(fd, socketName) }
        }.apply {
            isDaemon = true
            start()
        }

        return try {
            // 等待 native 端完成 bind/listen
            Thread.sleep(80)
            exec(
                context = context,
                binPath = binPath,
                command = listOf(exe.absolutePath) + args,
                extraEnv = mapOf("MOMIUI_USB_FD" to socketName),
                onLine = onLine,
            )
        } finally {
            runCatching { FdPass().stopServeLoop() }
            runCatching { server.interrupt() }
            runCatching { conn.close() }
            runCatching { ParcelFileDescriptor.adoptFd(fd).close() }
        }
    }

    private fun exec(
        context: Context,
        binPath: String,
        command: List<String>,
        extraEnv: Map<String, String>,
        onLine: (String) -> Unit,
    ): Int = try {
        val pb = ProcessBuilder(command).redirectErrorStream(true)
        // cwd 用可无障碍遍历的 home（/data/local/tmp/<pkg>），避免 getcwd 权限问题
        pb.directory(NativeLibs.homeDir(context))
        pb.environment().apply {
            putAll(NativeLibs.shellEnv(context, binPath, context.applicationInfo.nativeLibraryDir))
            putAll(extraEnv)
        }
        val p = pb.start()
        p.inputStream.bufferedReader().useLines { lines -> lines.forEach(onLine) }
        p.waitFor()
    } catch (t: Throwable) {
        onLine("[错误] ${t.javaClass.simpleName}: ${t.message}")
        -1
    }

    private fun quote(s: String): String =
        if (s.any { it.isWhitespace() || it == '"' || it == '\'' }) {
            "\"" + s.replace("\"", "\\\"") + "\""
        } else {
            s
        }
}
