package com.android.ozjflash.ui.util

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.ozjflash.ui.theme.LocalDialogStyle
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

/**
 * 界面换新 (Beta)：统一的「选项弹窗」入口，按 [LocalDialogStyle] 全局分派风格。
 *
 * - `miui`（默认）：Miuix [OverlayDropdownPreference]，即原来的 MIUI 风格下拉选项；
 * - `coui`：用 Miuix `ArrowPreference` 作触发行，弹出 COUI-Kit 的 [Menu]。
 *
 * COUI 弹窗的内部材质依赖 RuntimeShader，低版本统一回退到 MIUI 分支，避免出现
 * 空采样 / 崩溃。COUI 主题（`CouiTheme.isDark`）有 `isSystemInDarkTheme()` 兜底，
 * 菜单 backdrop 缺省时库会自动退化为纯色容器，不强制要求全局初始化。
 */
@Composable
fun StyledDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    startAction: @Composable () -> Unit = {},
    summary: String? = null,
) {
    val useCoui = LocalDialogStyle.current == "coui" &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    if (!useCoui) {
        OverlayDropdownPreference(
            title = title,
            summary = summary,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            startAction = startAction,
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val anchor = rememberCouiMenuAnchor()
    val current = items.getOrNull(selectedIndex) ?: ""

    Box(modifier = Modifier.couiMenuAnchor(anchor)) {
        ArrowPreference(
            title = title,
            summary = summary ?: current,
            startAction = startAction,
            onClick = { expanded = true },
            holdDownState = expanded,
        )
    }

    Menu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        items = items.mapIndexed { index, text ->
            CouiMenuItem(value = index, text = text, selected = index == selectedIndex)
        },
        onItemClick = { item ->
            expanded = false
            onSelectedIndexChange(item.value)
        },
        anchor = anchor,
        headerText = title,
    )
}
