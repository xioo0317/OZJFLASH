package com.android.ozjflash.flash

import android.content.Context
import com.Android.MoFlash.data.FdPass
import java.io.File

/**
 * 内置工具（fastboot / adb）命令行执行器 —— 完整复刻 MOFLASH 免 root 链路。
 *
 * ## 逆向结论（全部逐条取证于 MOFLASH.apk）
 *
 * ### 加密字符串表（qz.A → qz.m，逐字节 XOR）
 * 95 个 key 全部解开，与免 root 相关的只有两个：
 * - `k00` = **MOMIUI_USB_SOCK**
 * - `k01` = **MOMIUI_USB_FD**
 * 其余全是 fastboot/EDL/MTK 的命令与参数，无隐藏机制。
 *
 * ### fastboot 执行核心（qs0.n，dex 0x2a0e..），smali 直译
 * ```
 * socketName = "@momiui_fb_" + System.nanoTime()
 * env.put(MOMIUI_USB_SOCK, socketName)
 * env.put(MOMIUI_USB_FD,   String.valueOf(fd))
 * Thread t = new Thread(new Tt(fd, socketName), "fdpass-serve")  // daemon
 * t.start(); t.join(300)
 * Process p = ProcessBuilder(...).start(); p.waitFor()
 * finally { FdPass.INSTANCE.stopServeLoop(); t.join(1500) }
 * ```
 * `fd` = qs0.j，是**页面级常驻字段**，来自单例 UsbHostManager.currentFd()
 * = `UsbDeviceConnection.getFileDescriptor()`（公开 API，原始 usbfs fd）。
 *
 * ### adb 执行核心（Lp，dex 0x0f36..）
 * ```
 * adb start-server
 *   ADB_SERVER_SOCKET = localabstract:com.android.moflash.adb.noroot
 *   MOMIUI_USB_SOCK   = @momiui_adb_fd        (固定名)
 *   MOMIUI_USB_FD     = <常驻 fd>
 * ```
 * 之后命令都带 ADB_SERVER_SOCKET 作 client 连常驻 server；
 * 日志「usbhost fd 已保留，可直接重连」说明 fd 常驻、fdpass 固定名。
 *
 * ### 魔改二进制侧（libfastboot.so / libadb.so，与本项目 jniLibs 逐字节一致）
 * ```
 * p = getenv("MOMIUI_USB_SOCK"); q = getenv("MOMIUI_USB_FD")
 * if (p && p[0]=='@') {                       // 0x298a18 cmp w8,#0x40
 *     s = socket(AF_UNIX, SOCK_STREAM); connect(s, sun_path=p+1) 失败重试 101×20ms
 *     write(s, 1 byte); recvmsg(s, SCM_RIGHTS); fd = cmsg->fd
 * } else if (q) { fd = strtol(q) }           // 退化：仅同进程直传才有效
 * // 之后走 /proc/self/fd/<fd>，自己 detach_kernel_driver_and_claim
 * ```
 *
 * ### libfdpass.serveFdLoop（0xc50）
 * `socket→bind(@名)→listen(128)→[accept→poll→read(1B)→dup(fd)→sendmsg(SCM_RIGHTS)]×N`；
 * 全局唯一 listen fd，故本类用 [fdPassLock] 串行，`stopServeLoop` 用 shutdown 让其退出。
 *
 * ## 三条硬约束
 * 1. **不 claimInterface**：usbfs claim 跟文件句柄走，App 先 claim，子进程再 claim 会 EBUSY。
 * 2. **fd 常驻**：由 [UsbFdSession] 单例持有，公开 getFileDescriptor()，不反射/不 dup/不用完即关。
 * 3. **执行 nativeLibraryDir 里的真实 ELF**：Android 10+ 禁止 exec 应用私有目录文件。
 */
object ToolConsole {

    val SUPPORTED = setOf("fastboot", "adb")

    /** MOFLASH 同款 adb server 抽象 socket（noroot 通道） */
    private const val ADB_SERVER_SOCKET = "localabstract:com.android.ozjflash.adb.noroot"

    /** libfdpass 全局只有一个 listen fd，serveFdLoop 必须串行 */
    private val fdPassLock = Any()

    @Volatile
    private var adbServerStarted = false

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
     * @return 进程退出码；-1 表示空命令/二进制缺失/未授权/启动失败。
     */
    fun run(
        context: Context,
        raw: String,
        tool: String,
        rootMode: Boolean,
        onLine: (String) -> Unit,
    ): Int {
        // 命令日志：功能名_时间.log。诊断信息（info：…）只进日志不进终端；
        // 工具真实 stdout 既进终端也镜像进日志；执行指令写入日志便于排查。
        val logFile = CommandLogger.begin(context, tool)
        val diag: (String) -> Unit = { CommandLogger.append(logFile, it) }
        val out: (String) -> Unit = { line -> onLine(line); CommandLogger.append(logFile, line) }
        CommandLogger.append(logFile, "info：>_ ${tool} ${raw.trim()}")

        val args = normalize(raw, tool)
        if (args.isEmpty()) return -1

        val binPath = File(context.filesDir, "bin").absolutePath
        val exe = resolveExe(context, tool, binPath)
        if (exe == null) {
            out("[错误] 内置 $tool 不存在（lib/arm64 与 files/bin 均未找到）")
            return -1
        }

        // Root 模式：su -c 直接跑二进制，二进制自己能访问 /dev/bus/usb，无需 usbhost
        if (rootMode) {
            diag("info：root 模式：直接访问设备，无需 usbhost")
            return exec(
                context, binPath,
                listOf("su", "-c", exe.absolutePath + " " + args.joinToString(" ") { quote(it) }),
                emptyMap(), out,
            )
        }

        // —— 免 root：页面级常驻、已授权的 usbfs fd ——
        return when (val session = UsbFdSession.ensure(context, tool)) {
            is UsbFdSession.Result.NoDevice -> {
                if (tool == "fastboot") {
                    if (session.bus.isEmpty()) {
                        out("[错误] 未发现任何 USB 设备。请连接数据线并确认设备已进入 bootloader")
                    } else {
                        out("[错误] 未发现 fastboot 设备，当前 USB 总线：")
                        session.bus.forEach { out("        · $it") }
                        out("[提示] 需先进入 bootloader：adb reboot bootloader，或关机后按住音量下+电源键")
                    }
                    -1
                } else {
                    out("[提示] 未发现 ADB 模式 USB 设备，按本机模式执行")
                    exec(context, binPath, listOf(exe.absolutePath) + args, emptyMap(), out)
                }
            }
            is UsbFdSession.Result.NeedPermission -> {
                out("[提示] 已弹出系统 USB 授权框，请允许后重新执行（授权后页面会自动连接）")
                -1
            }
            is UsbFdSession.Result.OpenFailed -> {
                out("[错误] openDevice 失败或取不到 fd（UsbDeviceConnection.getFileDescriptor()）")
                -1
            }
            is UsbFdSession.Result.Ready -> {
                diag(UsbFdTracer.device(session.device, classifyDevice(session.device), granted = true))
                diag(UsbFdTracer.interfaces(session.device))
                diag(UsbFdTracer.fd(session.fd))
                diag("info：usbhost fd=${session.fd} ${if (session.reused) "已连接(常驻复用)" else "已连接(新建会话)"}")
                diag(UsbFdTracer.exe(exe.absolutePath))
                runNoRoot(context, exe, binPath, tool, args, session.fd, out, diag)
            }
        }
    }

    private fun runNoRoot(
        context: Context,
        exe: File,
        binPath: String,
        tool: String,
        args: List<String>,
        fd: Int,
        onLine: (String) -> Unit,
        diag: (String) -> Unit,
    ): Int = synchronized(fdPassLock) {
        val nativeErr = probeFdPass()
        if (nativeErr != null) {
            onLine("[错误] libfdpass 不可用：$nativeErr")
            return -1
        }

        if (tool == "adb") {
            // 首次：起常驻魔改 adb server（对齐 MOFLASH Lp）
            if (!adbServerStarted) {
                val code = execWithFdPass(
                    context, binPath, fd, "@momiui_adb_fd",
                    command = listOf(exe.absolutePath, "start-server"),
                    extraEnv = mapOf(
                        "ADB_SERVER_SOCKET" to ADB_SERVER_SOCKET,
                        "MOMIUI_USB_SOCK" to "@momiui_adb_fd",
                        "MOMIUI_USB_FD" to fd.toString(),
                    ),
                    label = "adb start-server", onLine = onLine, diag = diag,
                )
                if (code == 0) {
                    adbServerStarted = true
                    diag("info：adb server 就绪：$ADB_SERVER_SOCKET")
                } else {
                    diag("info：adb start-server 返回 $code，仍以完整 fd 注入方式执行命令")
                }
            }
            diag("info：执行：adb ${args.joinToString(" ")}")
            diag("info：ADB_SERVER_SOCKET=$ADB_SERVER_SOCKET  usbhost fd=$fd")
            // 每条命令仍自带完整 fdpass 自愈：即便 server 未常驻成功也能直连设备
            return execWithFdPass(
                context, binPath, fd, "@momiui_adb_fd",
                command = listOf(exe.absolutePath) + args,
                extraEnv = mapOf(
                    "ADB_SERVER_SOCKET" to ADB_SERVER_SOCKET,
                    "MOMIUI_USB_SOCK" to "@momiui_adb_fd",
                    "MOMIUI_USB_FD" to fd.toString(),
                ),
                label = "adb ${args.joinToString(" ")}", onLine = onLine, diag = diag,
            )
        }

        // —— fastboot：每命令 fdpass，随机 socket 名，注入两个环境变量 ——
        val socketName = "@momiui_fb_${System.nanoTime()}"
        diag(UsbFdTracer.socket(socketName))
        diag("info：执行：fastboot ${args.joinToString(" ")}")
        diag("info：MOMIUI_USB_SOCK=$socketName  MOMIUI_USB_FD=$fd")
        return execWithFdPass(
            context, binPath, fd, socketName,
            command = listOf(exe.absolutePath) + args,
            extraEnv = mapOf(
                "MOMIUI_USB_SOCK" to socketName,
                "MOMIUI_USB_FD" to fd.toString(),
            ),
            label = "fastboot ${args.joinToString(" ")}", onLine = onLine, diag = diag,
        )
    }

    /**
     * 起 fdpass 服务 → 注入环境变量 → exec → 停服务。
     * 完整对齐 MOFLASH qs0.n / Tt.run；fd 来自 [UsbFdSession] 常驻会话，用完不关。
     */
    private fun execWithFdPass(
        context: Context,
        binPath: String,
        fd: Int,
        socketName: String,
        command: List<String>,
        extraEnv: Map<String, String>,
        label: String,
        onLine: (String) -> Unit,
        diag: (String) -> Unit,
    ): Int {
        diag(UsbFdTracer.socket(socketName))
        val serverError = arrayOfNulls<String>(1)
        val server = Thread {
            try {
                FdPass().serveFdLoop(fd, socketName)
            } catch (t: Throwable) {
                serverError[0] = "${t.javaClass.simpleName}: ${t.message}"
            }
        }.apply {
            isDaemon = true
            name = "fdpass-serve"
            start()
        }
        return try {
            // MOFLASH 用 t.join(300)；这里轮询 /proc/net/unix 确认 bind/listen 就绪
            if (!waitSocketReady(socketName, 1500)) {
                diag("info：socket 未在 1.5s 内就绪，继续尝试（工具侧会自动重试 2s）")
            }
            exec(context, binPath, command, extraEnv, onLine)
        } finally {
            runCatching { FdPass().stopServeLoop() }
            runCatching { server.join(1500) }
            serverError[0]?.let { diag("info：fd 服务端异常：$it") }
            // 不关闭 UsbFdSession —— fd 常驻，供后续命令复用
        }
    }

    private fun resolveExe(context: Context, tool: String, binPath: String): File? {
        File(binPath, tool).takeIf { it.exists() }?.let { return it }
        val nativeDir = context.applicationInfo.nativeLibraryDir ?: return null
        return File(nativeDir, NativeLibs.libName(tool)).takeIf { it.exists() }
    }

    private fun probeFdPass(): String? = try {
        System.loadLibrary("fdpass")
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
