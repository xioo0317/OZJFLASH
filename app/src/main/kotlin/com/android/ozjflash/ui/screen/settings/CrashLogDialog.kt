package com.android.ozjflash.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.android.ozjflash.CrashLogger
import com.android.ozjflash.LogPackager
import com.android.ozjflash.R
import com.android.ozjflash.ozjApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 日志查看 / 复制 / 导出 / 清空对话框。
 *
 * 展示应用内全部 *.log 日志（不再只限于崩溃记录），并提供一个「导出」按钮，
 * 把日志打包成 zip 通过系统分享发送——照搬 KernelSU 的 exportLog 方案
 * （FileProvider 授权 + ACTION_SEND 选择器）。
 */
@Composable
fun CrashLogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(CrashLogger.readAll(ozjApp)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_log),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))

                if (text.isBlank()) {
                    Text(
                        text = stringResource(R.string.log_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        OutlinedButton(onClick = {
                            copyToClipboard(context, text)
                        }) {
                            Text(stringResource(R.string.log_copy))
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                val zip = withContext(Dispatchers.IO) { LogPackager.packageLogs(context) }
                                if (zip == null) {
                                    Toast.makeText(context, R.string.log_empty, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                shareZip(context, zip)
                            }
                        }) {
                            Text(stringResource(R.string.log_export))
                        }
                        OutlinedButton(onClick = {
                            CrashLogger.clearAll(ozjApp)
                            text = ""
                        }) {
                            Text(stringResource(R.string.log_clear))
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("ozjflash-log", text))
    Toast.makeText(context, R.string.log_copied, Toast.LENGTH_SHORT).show()
}

/** 把应用日志打包成 zip 并直接调起系统分享选择器（不再先弹查看对话框） */
fun shareLogsZip(context: Context) {
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    scope.launch {
        val zip = LogPackager.packageLogs(context)
        if (zip == null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, R.string.log_empty, Toast.LENGTH_SHORT).show()
            }
            return@launch
        }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            shareZip(context, zip)
        }
    }
}

private fun shareZip(context: Context, zip: java.io.File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.log_export)))
        Toast.makeText(context, R.string.log_export_success, Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, R.string.log_export_failed, Toast.LENGTH_SHORT).show()
    }
}
