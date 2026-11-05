package com.android.ozjflash.flash

import android.content.Context
import java.io.File

/**
 * Fastboot 命令行执行器。
 *
 * 设计要点（与 MOFLASH 的「应用内直接运行二进制工具」保持一致）：
 *  - 每条命令都通过 ProcessBuilder **直接 exec 内置 fastboot 二进制**，执行完进程即退出；
 *  - 不依赖常驻 bash / PTY，因此不存在「终端被某条命令占用后连不上」的问题；
 *  - 输入支持 `devices` 与 `fastboot devices` 两种写法；
 *  - rootMode=true 时用 `su -c` 执行，使内置 fastboot 取得 USB 访问能力；
 *    免 root 时直接执行（需系统侧已授予 USB 权限）。
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
     * @return 进程退出码；-1 表示空命令 / 二进制缺失 / 启动失败。
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

        val command: List<String> = if (rootMode) {
            listOf("su", "-c", exe.absolutePath + " " + args.joinToString(" ") { quote(it) })
        } else {
            listOf(exe.absolutePath) + args
        }

        return try {
            val pb = ProcessBuilder(command).redirectErrorStream(true)
            // cwd 用可无障碍遍历的 home（/data/local/tmp/<pkg>），避免 getcwd 权限问题
            pb.directory(NativeLibs.homeDir(context))
            pb.environment().putAll(
                NativeLibs.shellEnv(context, binPath, context.applicationInfo.nativeLibraryDir)
            )
            val p = pb.start()
            p.inputStream.bufferedReader().useLines { lines -> lines.forEach(onLine) }
            p.waitFor()
        } catch (t: Throwable) {
            onLine("[错误] ${t.javaClass.simpleName}: ${t.message}")
            -1
        }
    }

    private fun quote(s: String): String =
        if (s.any { it.isWhitespace() || it == '"' || it == '\'' }) {
            "\"" + s.replace("\"", "\\\"") + "\""
        } else {
            s
        }
}
