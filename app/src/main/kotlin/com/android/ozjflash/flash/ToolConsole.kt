package com.android.ozjflash.flash

import android.content.Context
import com.Android.MoFlash.data.FdPass
import java.io.File

/**
 * 内置工具（fastboot / adb）命令行执行器。
 *
 * ## 免 root 链路（逆向 MOFLASH libfastboot.so 反汇编得到，逐条对应）
 *
 * ```
 * p = getenv("MOMIUI_USB_SOCK")            // 0x2989e8，主路径
 * q = getenv("MOMIUI_USB_FD")              // 0x2989fc，退化路径
 * if (p && p[0] == '@') {                  // 0x298a18  cmp w8, #0x40
 *     s = socket(AF_UNIX, SOCK_STREAM, 0)  // 0x298a20
 *     connect(s, sun_path = p + 1)         // 0x298a9c，失败重试 101 次 × 20ms
 *     write(s, 1 byte)                     // 0x298afc
 *     recvmsg(s, SCM_RIGHTS)               // 0x298b54
 *     fd = cmsg->fd                        // 0x298b88
 *     // 之后全部走 /proc/self/fd/<fd>
 * } else if (q) { fd = strtol(q) }         // 0x298bc4，同进程直传才有效
 * ```
 *
 * 服务端 [FdPass.serveFdLoop] 与之配对：
 * `socket → bind(@名) → listen → accept → poll → read(1B) → dup(fd) → sendmsg(SCM_RIGHTS)`。
 * 名字必须 `@` 开头（否则 native 直接返回 -2）。
 *
 * ## 三条硬约束（踩过的坑）
 *
 *  1. **不要 claimInterface**：usbfs 的 claim 跟着文件句柄走，App 先 claim，
 *     子进程再 claim 会 EBUSY → `< waiting for any device >`。
 *  2. **fd 不能是临时对象**：必须 `Os.dup()` 拿裸 int，用
 *     `ParcelFileDescriptor.dup().fd` 会被 GC finalize 关掉。
 *  3. **执行的是 nativeLibraryDir 里的真实 ELF**：Android 10+ 禁止 exec
 *     应用私有目录下的文件，符号链接解析到 `/data/app/.../lib/arm64` 才允许。
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
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val exe = resolveExe(context, tool, binPath)
        if (exe == null) {
            onLine("[错误] 内置 $tool 不存在（lib/arm64 与 files/bin 均未找到）")
            return -1
        }

        if (rootMode) {
            val command = listOf(
                "su", "-c",
                exe.absolutePath + " " + args.joinToString(" ") { quote(it) },
            )
            onLine("[诊断] root 模式，直接执行：${exe.absolutePath}")
            return exec(context, binPath, command, emptyMap(), onLine)
        }

        // —— 免 root：需要一个已授权的 USB fd ——
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
        onLine("[诊断] 可执行文件 = ${exe.absolutePath}")

        val socketName = socketNameFor(tool)
        onLine(UsbFdTracer.socket(socketName))

        // native 侧是否真的可用，必须在起线程前确认，否则失败会被静默吞掉
        val nativeErr = probeFdPass()
        if (nativeErr != null) {
            onLine("[错误] libfdpass 不可用：$nativeErr")
            runCatching { conn.close() }
            runCatching { releaseFd(fd) }
            return -1
        }

        val serverError = arrayOfNulls<String>(1)
        val server = Thread {
            try {
                FdPass().serveFdLoop(fd, socketName)
            } catch (t: Throwable) {
                serverError[0] = "${t.javaClass.simpleName}: ${t.message}"
            }
        }.apply {
            isDaemon = true
            start()
        }

        return try {
            // 等服务端真正 bind+listen 完成（读 /proc/net/unix 轮询，比固定 sleep 可靠）
            val ready = waitSocketReady(socketName, 1500)
            if (!ready) {
                onLine("[警告] 未在 1.5s 内观察到 socket 就绪，继续尝试（工具侧会自动重试 2s）")
            }
            onLine("[诊断] 执行: $tool ${args.joinToString(" ")}")
            onLine("[诊断] MOMIUI_USB_SOCK=$socketName  MOMIUI_USB_FD=$fd")
            exec(
                context = context,
                binPath = binPath,
                command = listOf(exe.absolutePath) + args,
                extraEnv = mapOf(
                    // 魔改版主路径：@ 开头的抽象 socket 名
                    "MOMIUI_USB_SOCK" to socketName,
                    // 退化路径：同进程内可直接用 fd 号；跨进程以 SOCK 为准
                    "MOMIUI_USB_FD" to fd.toString(),
                ),
                onLine = onLine,
            )
        } finally {
            runCatching { FdPass().stopServeLoop() }
            runCatching { server.interrupt() }
            runCatching { conn.close() }
            runCatching { releaseFd(fd) }
            serverError[0]?.let { onLine("[错误] fd 服务端异常：$it") }
        }
    }

    /**
     * 解析要执行的二进制。
     *
     * 优先 symbol 链接（files/bin/<tool> → nativeLibraryDir/libozj_*.so），
     * 链接不存在时直接退回 nativeLibraryDir 里的 libozj_*.so 全名 ——
     * MOFLASH 日志里跑的就是 `/data/app/.../lib/arm64/libfastboot.so`。
     */
    private fun resolveExe(context: Context, tool: String, binPath: String): File? {
        File(binPath, tool).takeIf { it.exists() }?.let { return it }
        val nativeDir = context.applicationInfo.nativeLibraryDir ?: return null
        val lib = File(nativeDir, NativeLibs.libName(tool))
        return lib.takeIf { it.exists() }
    }

    /** MOFLASH 同款 socket 名前缀：fastboot 带随机后缀，adb 用固定名 */
    private fun socketNameFor(tool: String): String =
        if (tool == "adb") "@momiui_adb_fd" else "@momiui_fb_${System.nanoTime()}"

    /** 尝试加载 fdpass，返回错误描述；可用时返回 null */
    private fun probeFdPass(): String? = try {
        // loadLibrary 幂等：已加载时是 no-op，加载失败会抛，正好用来探测
        System.loadLibrary("fdpass")
        // 再确认符号确实在（防止库在但被 strip / ABI 不匹配）
        FdPass::class.java.getDeclaredMethod(
            "serveFdLoop",
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        null
    } catch (t: Throwable) {
        val cause = t.cause ?: t
        "${cause.javaClass.simpleName}: ${cause.message}"
    }

    /** 抽象 socket 会出现在 /proc/net/unix 里，形如 `@名字`；读不到就退化为固定等待 */
    private fun waitSocketReady(name: String, timeoutMs: Long): Boolean {
        val key = "@" + name.removePrefix("@")
        val deadline = System.currentTimeMillis() + timeoutMs
        var readable = false
        while (System.currentTimeMillis() < deadline) {
            val hit = runCatching {
                val lines = File("/proc/net/unix").readLines()
                readable = true
                lines.any { it.trim().endsWith(key) }
            }.getOrDefault(false)
            if (hit) return true
            if (!readable) break
            Thread.sleep(25)
        }
        if (!readable) Thread.sleep(200)
        return false
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
