package com.android.ozjflash.flash

import android.content.Context
import android.os.ParcelFileDescriptor
import com.Android.MoFlash.data.FdPass
import java.io.File

/**
 * 内置工具（fastboot / adb）命令行执行器。
 *
 * 执行模型（与 MOFLASH 一致）：
 *  - **root**：`su -c "<bin>/<tool> ..."`，由 su 提供设备访问能力；
 *  - **免 root**：`UsbManager.openDevice` 拿到已授权的 usbfs fd →
 *    `FdPass.serveFdLoop(fd, "@名字")` 起 Unix 抽象 socket 服务端 →
 *    exec 魔改版工具，环境变量 `MOMIUI_USB_SOCK="@名字"` →
 *    工具 connect 该 socket，用 SCM_RIGHTS 收取 fd，走 `/proc/self/fd/<n>` 直接通信。
 *
 * 关键细节：
 *  - 环境变量是 **MOMIUI_USB_SOCK**（`@` 开头），不是 MOMIUI_USB_FD；
 *    MOMIUI_USB_FD 只是「同进程直传 fd 号」的退化路径，跨进程无效；
 *  - **不要 claimInterface**：usbfs 的 claim 跟着文件句柄走，App 先 claim，
 *    子进程再 claim 会 EBUSY，表现为 `< waiting for any device >`；
 *  - 每条命令一个独立短命进程，跑完即退，不占用常驻 bash / PTY。
 */
object ToolConsole {

    /** 支持命令行的工具 */
    val SUPPORTED = setOf("fastboot", "adb")

    /** 去掉可选的工具名前缀，并做带引号的分词 */
    fun normalize(raw: String, tool: String): List<String> {
        val t = raw.trim()
        if (t.isEmpty()) return emptyList()
        val body = when {
            t == tool -> ""
            t.startsWith("$tool ") -> t.removePrefix(tool).trim()
            else -> t
        }
        return if (body.isEmpty()) emptyList() else splitArgs(body)
    }

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
     * 执行一条命令，逐行回调输出。
     *
     * @param tool `fastboot` 或 `adb`
     * @return 进程退出码；-1 表示空命令 / 二进制缺失 / 未授权 / 启动失败。
     */
    fun run(
        context: Context,
        raw: String,
        tool: String,
        rootMode: Boolean,
        onLine: (String) -> Unit,
    ): Int {
        val args = normalize(raw, tool)
        if (args.isEmpty()) return -1

        val binPath = File(context.filesDir, "bin").absolutePath
        val exe = File(binPath, tool)
        if (!exe.exists()) {
            onLine("[错误] 内置 $tool 不存在：${exe.absolutePath}")
            return -1
        }

        if (rootMode) {
            val command = listOf(
                "su", "-c",
                exe.absolutePath + " " + args.joinToString(" ") { quote(it) },
            )
            return exec(context, binPath, command, emptyMap(), onLine)
        }

        // —— 免 root：都需要一个已授权的 USB fd ——
        val device = UsbHost.pickForTool(context, tool)
        if (device == null) {
            if (tool == "fastboot") {
                val others = UsbHost.describeAll(context)
                if (others.isEmpty()) {
                    onLine("[错误] 未发现任何 USB 设备。请连接数据线，并确认已开启 USB 调试")
                } else {
                    onLine("[错误] 未发现 fastboot 设备，当前 USB 总线：")
                    others.forEach { onLine("        · $it") }
                    onLine("[提示] 设备需先进入 bootloader：adb reboot bootloader，或关机后按住音量下+电源键")
                }
                return -1
            }
            // adb 没有 OTG 设备也能用（本机/网络模式），直接执行，不注入 fd
            onLine("[提示] 未发现 ADB 模式的 USB 设备，按本机模式执行")
            return exec(context, binPath, listOf(exe.absolutePath) + args, emptyMap(), onLine)
        }

        val manager = UsbHostManager(context)
        val granted = manager.hasPermission(device)
        onLine(UsbFdTracer.device(device, classifyDevice(device), granted))
        onLine(UsbFdTracer.interfaces(device))
        if (!granted) {
            manager.requestPermission(device)
            onLine("[提示] 尚未授权 USB 访问，已弹出系统授权框，请允许后重新执行")
            return -1
        }

        // 刻意不 claimInterface —— 交给子进程里的工具自己 claim
        val pair = manager.openWithFd(device)
        if (pair == null) {
            onLine("[错误] 打开 USB 设备失败（openDevice 返回 null 或取不到 fd）")
            return -1
        }
        val (conn, fd) = pair
        onLine(UsbFdTracer.fd(fd))

        val socketName = "@OZJ_USB_FD_${System.nanoTime()}"
        onLine(UsbFdTracer.socket(socketName))
        val server = Thread {
            runCatching { FdPass().serveFdLoop(fd, socketName) }
        }.apply {
            isDaemon = true
            start()
        }

        return try {
            // 等 native 端完成 bind/listen（工具侧 connect 本身有 101 次 × 20ms 重试，这里只是省掉首轮空转）
            Thread.sleep(120)
            onLine("[诊断] 执行: $tool ${args.joinToString(" ")}")
            exec(
                context = context,
                binPath = binPath,
                command = listOf(exe.absolutePath) + args,
                extraEnv = mapOf(
                    // 魔改版优先读这个（@ 开头的抽象 socket 名）
                    "MOMIUI_USB_SOCK" to socketName,
                    // 退化路径：同进程内可直接用 fd 号；跨进程时以 SOCK 为准
                    "MOMIUI_USB_FD" to fd.toString(),
                ),
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
        // cwd 用可无障碍遍历的 home，避免 getcwd 权限问题
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
