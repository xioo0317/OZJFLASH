package com.android.ozjflash.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.material.ExpressiveDialog
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 说明书的一节：小标题 + 正文。 */
private data class ManualSection(val title: String, val body: String)

/**
 * ADB 二级页「?」圆钮点击后弹出的说明书。
 * 双风格对应：Miuix 用 OverlayDialog，Material3 用 ExpressiveDialog，
 * 与 USB 设备信息弹窗保持一致；内容较长故整体可滚动。
 */
@Composable
fun AdbManualDialog(onDismiss: () -> Unit) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> AdbManualDialogMiuix(onDismiss)
        UiMode.Material -> AdbManualDialogMaterial(onDismiss)
    }
}

@Composable
private fun manualSections(): List<ManualSection> = listOf(
    ManualSection(
        stringResource(R.string.adb_manual_s1_title),
        stringResource(R.string.adb_manual_s1_body),
    ),
    ManualSection(
        stringResource(R.string.adb_manual_s2_title),
        stringResource(R.string.adb_manual_s2_body),
    ),
    ManualSection(
        stringResource(R.string.adb_manual_s3_title),
        stringResource(R.string.adb_manual_s3_body),
    ),
    ManualSection(
        stringResource(R.string.adb_manual_s4_title),
        stringResource(R.string.adb_manual_s4_body),
    ),
    ManualSection(
        stringResource(R.string.adb_manual_s5_title),
        stringResource(R.string.adb_manual_s5_body),
    ),
)

@Composable
private fun AdbManualDialogMiuix(onDismiss: () -> Unit) {
    val sections = manualSections()
    val scheme = MiuixTheme.colorScheme
    OverlayDialog(
        show = true,
        title = stringResource(R.string.adb_manual_title),
        summary = stringResource(R.string.adb_manual_summary),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            sections.forEach { section ->
                MiuixText(
                    text = section.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                MiuixText(
                    text = section.body,
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(12.dp))
            }
            MiuixTextButton(
                text = stringResource(R.string.adb_manual_close),
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun AdbManualDialogMaterial(onDismiss: () -> Unit) {
    val sections = manualSections()
    val scheme = MaterialTheme.colorScheme
    ExpressiveDialog(
        onDismissRequest = onDismiss,
        title = { M3Text(stringResource(R.string.adb_manual_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                sections.forEach { section ->
                    M3Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                    )
                    Spacer(Modifier.height(3.dp))
                    M3Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            M3TextButton(onClick = onDismiss) {
                M3Text(stringResource(R.string.adb_manual_close))
            }
        },
    )
}
