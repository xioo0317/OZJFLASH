package com.android.ozjflash.ui.util

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.ozjflash.ui.theme.LocalDropdownStyle
import com.itos.coui.menu.CouiMenuDefaults
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

/**
 * 统一「选项卡片」入口，由 [LocalDropdownStyle] 决定弹窗风格：
 *
 * - `"miuix"`：Miuix [OverlayDropdownPreference]（原厂下拉，锚定在触发行下方展开）。
 * - `"coui"`：ColorOS 17 风格磨砂透背景卡片（COUI [Menu]），API 33 以下自动回退 Miuix。
 *
 * COUI 卡片细节（贴合 ColorOS 17）：
 * - **位置**：通过 `anchor.open { expanded = true }` 在点击抬起时把「按下位置」转存为
 *   持久点击位置；若在 onClick 里直接置 `expanded = true`，COUI 的 pendingTouchPosition
 *   已在抬起后清空，菜单会回退到窗口左下角。这里改为 `anchor.open` 让菜单出现在触点附近。
 * - **宽度**：`widthIn(min = 200.dp, max = 300.dp)`，比默认 128~200dp 更宽，接近参考图。
 * - **确认蓝色**：覆盖默认 `primary` 为主题无关的 ColorOS 蓝，选中/按下反馈均为蓝色。
 * - **去当前选中标记**：菜单项不传 `selected`，不显示「当前选中」的勾选/背景高亮提醒。
 *
 * 背景透模糊依赖窗口级 `ProvideCouiMenuBackdrop`（MainActivity 提供）。
 */
@Composable
fun CouiDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    startAction: @Composable () -> Unit = {},
    summary: String? = null,
) {
    val useCoui = LocalDropdownStyle.current == "coui" &&
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
            onClick = { anchor.open { expanded = true } },
            holdDownState = expanded,
        )
    }

    // 确认/选中反馈统一用 ColorOS 蓝，与当前主题色解耦。
    val baseStyle = CouiMenuDefaults.style()
    val style = remember(baseStyle) {
        baseStyle.copy(colors = baseStyle.colors.copy(primary = Color(0xFF3B82F6)))
    }

    Menu(
        modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
        expanded = expanded,
        onDismissRequest = { expanded = false },
        // 不传 selected：不显示「当前选中项」的勾选/高亮提醒。
        items = items.mapIndexed { index, text ->
            CouiMenuItem(value = index, text = text)
        },
        onItemClick = { item ->
            expanded = false
            onSelectedIndexChange(item.value)
        },
        style = style,
        anchor = anchor,
    )
}
