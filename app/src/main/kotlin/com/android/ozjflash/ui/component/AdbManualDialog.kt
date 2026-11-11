package com.android.ozjflash.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ozjflash.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 说明书的一节：小标题 + 正文。 */
private data class ManualSection(val title: String, val body: String)

/**
 * 命令行二级页「?」圆钮点击后弹出的说明书。
 *
 * [fastboot] 为 true 时显示 Fastboot 专属文案（引导模式 / 刷写命令 / 免 Root 原理），
 * 为 false 时显示 ADB 文案（USB 调试 / adb 子命令 / 免 Root 原理）。弹窗外壳用
 * Miuix 的 OverlayDialog，与 USB 设备信息弹窗保持一致；内容较长故整体可滚动。
 */
@Composable
fun ToolManualDialog(fastboot: Boolean, onDismiss: () -> Unit) {
    ToolManualDialogMiuix(fastboot, onDismiss)
}

@Composable
private fun manualSections(fastboot: Boolean): List<ManualSection> = if (fastboot) {
    listOf(
        ManualSection(
            stringResource(R.string.fastboot_manual_s1_title),
            stringResource(R.string.fastboot_manual_s1_body),
        ),
        ManualSection(
            stringResource(R.string.fastboot_manual_s2_title),
            stringResource(R.string.fastboot_manual_s2_body),
        ),
        ManualSection(
            stringResource(R.string.fastboot_manual_s3_title),
            stringResource(R.string.fastboot_manual_s3_body),
        ),
        ManualSection(
            stringResource(R.string.fastboot_manual_s4_title),
            stringResource(R.string.fastboot_manual_s4_body),
        ),
        ManualSection(
            stringResource(R.string.fastboot_manual_s5_title),
            stringResource(R.string.fastboot_manual_s5_body),
        ),
    )
} else {
    listOf(
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
}

private fun manualTitleRes(fastboot: Boolean) =
    if (fastboot) R.string.fastboot_manual_title else R.string.adb_manual_title

private fun manualSummaryRes(fastboot: Boolean) =
    if (fastboot) R.string.fastboot_manual_summary else R.string.adb_manual_summary

@Composable
private fun ToolManualDialogMiuix(fastboot: Boolean, onDismiss: () -> Unit) {
    val sections = manualSections(fastboot)
    val scheme = MiuixTheme.colorScheme
    OverlayDialog(
        show = true,
        title = stringResource(manualTitleRes(fastboot)),
        summary = stringResource(manualSummaryRes(fastboot)),
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
