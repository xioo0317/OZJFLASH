package com.android.ozjflash.ui.screen.feature

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.component.GlassBackButton
import com.android.ozjflash.ui.component.TerminalOutputWindow
import com.android.ozjflash.flash.NativeLibs
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.material.ExpressiveScaffold
import com.android.ozjflash.ui.component.material.expressiveTopAppBarColors
import com.android.ozjflash.ui.navigation3.Navigator
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.util.BlurredBar
import com.android.ozjflash.ui.util.rememberBlurBackdrop
import com.android.ozjflash.ui.util.rememberCouiBlurBackdrop
import com.android.ozjflash.ui.util.couiBlurSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/**
 * 功能页两个解压入口共用的类型。
 *
 * 二进制走 nativeLibraryDir 里的 lib*.so（命令名格式），运行时由
 * [NativeLibs.prepare] 在 files/bin 下建好干净工具名的符号链接。
 * 7z 对应 lib7zz.so → 7z，payload 对应 libpayload_dumper_go.so → payload-dumper-go。
 */
enum class ExtractKind(
    @StringRes val titleRes: Int,
    val bin: String,
    val libFile: String,
    @DrawableRes val iconRes: Int,
) {
    SEVEN_Z(R.string.feature_7z, "7z", "lib7zz.so", R.drawable.ic_feature_7z),
    PAYLOAD(
        R.string.feature_payload,
        "payload-dumper-go",
        NativeLibs.libName("payload-dumper-go"),
        R.drawable.ic_feature_payload,
    ),
}

/* ------------------------------ 执行器 ------------------------------ */

private object ExtractRunner {

    data class Result(val code: Int, val outDir: String)

    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }.getOrNull()

    /** 内部存储文档直接还原成真实路径；其它 provider 复制到 cache，供 ELF 读取 */
    private fun resolveInput(
        context: Context,
        uri: Uri,
        name: String,
        onLine: (String) -> Unit,
    ): File? {
        if (uri.authority == "com.android.externalstorage.documents") {
            runCatching {
                val sub = DocumentsContract.getDocumentId(uri).substringAfter(':')
                if (sub.isNotEmpty()) {
                    val direct = File("/storage/emulated/0", sub)
                    if (direct.isFile && direct.canRead()) return direct
                }
            }
        }
        onLine(context.getString(R.string.extract_preparing))
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dir = File(context.cacheDir, "extract_in").apply { mkdirs() }
        val out = File(dir, safe)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            } ?: return null
            out
        } catch (t: Throwable) {
            onLine("[错误] ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    private fun outDir(context: Context, name: String): File {
        val stem = name.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._-]"), "_")
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "extract/$stem").apply { mkdirs() }
    }

    fun run(
        context: Context,
        kind: ExtractKind,
        uri: Uri,
        onLine: (String) -> Unit,
    ): Result {
        val binPath = File(context.filesDir, "bin").absolutePath
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val exe = File(binPath, kind.bin).takeIf { it.exists() }
            ?: nativeDir?.let { File(it, kind.libFile) }?.takeIf { it.exists() }
        if (exe == null) {
            onLine(context.getString(R.string.extract_missing_binary, kind.bin))
            return Result(-1, "")
        }

        val name = displayName(context, uri) ?: uri.lastPathSegment ?: "input.bin"
        val input = resolveInput(context, uri, name, onLine)
            ?: return Result(-1, "")

        val out = outDir(context, name)
        onLine(context.getString(R.string.extract_output_dir, out.absolutePath))

        val command = when (kind) {
            // 7zz: -o 后紧跟输出目录（无空格）
            ExtractKind.SEVEN_Z -> listOf(
                exe.absolutePath, "x", "-y", "-o" + out.absolutePath, input.absolutePath,
            )
            // payload-dumper-go: -o <dir> <payload.bin>
            ExtractKind.PAYLOAD -> listOf(
                exe.absolutePath, "-o", out.absolutePath, input.absolutePath,
            )
        }
        onLine("> " + command.joinToString(" "))

        return try {
            val pb = ProcessBuilder(command).redirectErrorStream(true)
            pb.directory(NativeLibs.homeDir(context))
            pb.environment().putAll(NativeLibs.shellEnv(context, binPath, nativeDir))
            val p = pb.start()
            p.inputStream.bufferedReader().useLines { lines -> lines.forEach(onLine) }
            Result(p.waitFor(), out.absolutePath)
        } catch (t: Throwable) {
            onLine("[错误] ${t.javaClass.simpleName}: ${t.message}")
            Result(-1, out.absolutePath)
        }
    }
}

@Stable
private class ExtractState(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    var uri by mutableStateOf<Uri?>(null)
    var name by mutableStateOf<String?>(null)
    var output by mutableStateOf("")
    var outPath by mutableStateOf<String?>(null)
    var running by mutableStateOf(false)

    fun onPicked(newUri: Uri?) {
        if (newUri == null) return
        uri = newUri
        name = ExtractRunner.displayName(context, newUri) ?: newUri.lastPathSegment
    }

    fun start(kind: ExtractKind) {
        val picked = uri ?: return
        if (running) return
        running = true
        output = ""
        outPath = null
        scope.launch {
            val sb = StringBuilder()
            val result = withContext(Dispatchers.IO) {
                ExtractRunner.run(context, kind, picked) { line ->
                    val snapshot = synchronized(sb) {
                        sb.appendLine(line)
                        sb.toString()
                    }
                    scope.launch(Dispatchers.Main.immediate) { output = snapshot }
                }
            }
            if (result.outDir.isNotEmpty()) outPath = result.outDir
            output = buildString {
                append(sb)
                if (isNotEmpty() && !endsWith("\n")) append('\n')
                append(if (result.code == 0) "✔ exit=0" else "✘ exit=${result.code}")
            }
            running = false
        }
    }
}

@Composable
private fun rememberExtractState(): ExtractState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) { ExtractState(context.applicationContext, scope) }
}

/* ------------------------------ 页面 ------------------------------ */

/** 解压二级页：按当前 UI 主体分派到 Miuix / Material 两套外壳 */
@Composable
fun FeatureExtractScreen(navigator: Navigator, kind: ExtractKind) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ExtractMiuix(navigator, kind)
        UiMode.Material -> ExtractMaterial(navigator, kind)
    }
}

@Composable
private fun ExtractMiuix(navigator: Navigator, kind: ExtractKind) {
    val scrollBehavior = MiuixScrollBehavior(
        rememberMiuixTopAppBarState(),
        { true },
        spring<Float>(),
        exponentialDecay<Float>(),
    )
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val couiBackdrop = rememberCouiBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop, couiBackdrop = couiBackdrop) {
                MiuixTopAppBar(
                    color = barColor,
                    title = stringResource(kind.titleRes),
                    navigationIcon = {
                        GlassBackButton(
                            imageVector = MiuixIcons.Back,
                            onClick = { navigator.pop() },
                            mirrorForRtl = true,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .couiBlurSource(couiBackdrop)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(12.dp),
        ) {
            ExtractBodyMiuix(kind)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractMaterial(navigator: Navigator, kind: ExtractKind) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val topInsets = WindowInsets.systemBars
        .add(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    ExpressiveScaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(kind.titleRes)) },
                navigationIcon = {
                    GlassBackButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(),
                windowInsets = topInsets,
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = topInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            ExtractBodyMaterial(kind)
        }
    }
}

@Composable
private fun ExtractBodyMiuix(kind: ExtractKind) {
    val state = rememberExtractState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        state.onPicked(it)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MiuixCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiuixText(
                    text = stringResource(R.string.extract_hint),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                MiuixText(
                    text = state.name ?: stringResource(R.string.extract_no_file),
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                MiuixButton(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MiuixText(stringResource(R.string.extract_pick_file))
                }
                MiuixButton(
                    onClick = { state.start(kind) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.uri != null && !state.running,
                ) {
                    MiuixText(
                        if (state.running) stringResource(R.string.extract_running)
                        else stringResource(R.string.extract_start)
                    )
                }
                state.outPath?.let {
                    MiuixText(
                        text = stringResource(R.string.extract_output_dir, it),
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        TerminalOutputWindow(
            output = state.output,
            emptyText = stringResource(R.string.extract_output_empty),
        )
    }
}

@Composable
private fun ExtractBodyMaterial(kind: ExtractKind) {
    val state = rememberExtractState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        state.onPicked(it)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.extract_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.name ?: stringResource(R.string.extract_no_file),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.extract_pick_file))
                }
                Button(
                    onClick = { state.start(kind) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.uri != null && !state.running,
                ) {
                    Text(
                        if (state.running) stringResource(R.string.extract_running)
                        else stringResource(R.string.extract_start)
                    )
                }
                state.outPath?.let {
                    Text(
                        text = stringResource(R.string.extract_output_dir, it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        TerminalOutputWindow(
            output = state.output,
            emptyText = stringResource(R.string.extract_output_empty),
        )
    }
}
