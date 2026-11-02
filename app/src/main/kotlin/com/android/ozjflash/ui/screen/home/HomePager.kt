package com.android.ozjflash.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import com.android.ozjflash.CrashLogger
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.ozjApp
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import java.io.File

/**
 * 主页状态。完全剥离 KernelSU 的 root / 内核态逻辑（ksuVersion、LKM、jailbreak…），
 * 改为展示设备信息与内置刷写工具链状态 —— 这正好对应 OZJFLASH “免 root、自带工具”的定位。
 */
@Immutable
data class HomeUiState(
    val appVersion: String,
    val androidVersion: String,
    val sdk: Int,
    val deviceModel: String,
    val fingerprint: String,
    val abi: String,
    val selinuxStatus: String,
    val toolsTotal: Int,
    val toolsPresent: Int,
    val toolsMissing: List<String>,
    val hasCrash: Boolean,
) {
    val toolsReady: Boolean get() = toolsMissing.isEmpty()
}

@Immutable
data class HomeActions(
    val onOpenUrl: (String) -> Unit,
)

@Composable
fun rememberHomeState(binDir: String): HomeUiState {
    val context = LocalContext.current
    return remember(binDir) {
        val missing = NativeLibs.missingTools(binDir)
        HomeUiState(
            appVersion = readAppVersion(context),
            androidVersion = android.os.Build.VERSION.RELEASE,
            sdk = android.os.Build.VERSION.SDK_INT,
            deviceModel = android.os.Build.MODEL,
            fingerprint = android.os.Build.FINGERPRINT,
            abi = android.os.Build.SUPPORTED_ABIS.joinToString(),
            selinuxStatus = readSelinuxStatus(),
            toolsTotal = NativeLibs.TOOLS.size,
            toolsPresent = NativeLibs.TOOLS.size - missing.size,
            toolsMissing = missing,
            hasCrash = CrashLogger.exists(ozjApp),
        )
    }
}

private fun readAppVersion(context: Context): String = try {
    @Suppress("DEPRECATION")
    val pi = context.packageManager.getPackageInfo(context.packageName, 0)
    val name = pi.versionName ?: "?"
    val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        pi.longVersionCode.toString()
    } else {
        @Suppress("DEPRECATION") pi.versionCode.toString()
    }
    "$name ($code)"
} catch (_: Throwable) {
    "?"
}

private fun readSelinuxStatus(): String = try {
    val v = File("/sys/fs/selinux/enforce").readText().trim()
    when (v) {
        "0" -> "Permissive"
        "1" -> "Enforcing"
        else -> "Unknown"
    }
} catch (_: Throwable) {
    "Unknown"
}

@Composable
fun HomePager(
    binDir: String,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val uriHandler = LocalUriHandler.current
    val actions = HomeActions(onOpenUrl = uriHandler::openUri)

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(
            state = rememberHomeState(binDir),
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> HomePagerMaterial(
            state = rememberHomeState(binDir),
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}
