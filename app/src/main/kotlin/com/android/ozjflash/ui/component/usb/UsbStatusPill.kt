package com.android.ozjflash.ui.component.usb

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ozjflash.R
import com.android.ozjflash.flash.UsbDeviceInfo
import com.android.ozjflash.flash.UsbHost
import com.android.ozjflash.flash.UsbMode
import com.android.ozjflash.ui.LocalUiMode
import com.android.ozjflash.ui.UiMode
import com.android.ozjflash.ui.component.liquid.lens
import com.android.ozjflash.ui.component.liquid.vibrancy
import com.android.ozjflash.ui.theme.LocalBlurStyle
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.android.ozjflash.ui.theme.LocalUsbPillBlur
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 图二那种「玻璃圆圈」的白色高光环：BloomStroke 双峰光源，上缘主光 + 下缘副光，
 * 沿胶囊轮廓勾一圈明亮描边，是液态玻璃观感最关键的一层。
 */
private val PillGlassHighlight = Highlight(
    width = 1.2.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.38f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.82f, -0.5f),
            color = Color.White,
            intensity = 0.5f,
        ),
        dualPeak = true,
    ),
)

/**
 * 顶栏 USB 状态胶囊。
 *
 * - **风格对应组件**：MIUI 主体用 Miuix [MiuixSurface]，M3 主体用 Material3 [M3Surface]，
 *   点击反馈由各自组件自带；文字 / 图标也分别用 MiuixText + MiuixIcon 与 M3Text + Icon。
 * - **主题取色**：容器与文字颜色全部取自当前 colorScheme（浅色=白、深色/纯黑=黑、
 *   动态取色时自动带主题色），不再硬编码灰底黑字。胶囊文案统一为「已连接设备」，
 *   避免在可折叠顶栏收起时挤占小标题；设备详情点击胶囊在下拉里查看。
 * - **液态玻璃**：主题设置「胶囊模糊」开启后，用 Miuix 的 drawBackdrop + blur/lens/vibrancy
 *   把胶囊折射成玻璃（与悬浮底栏同款效果），API < 33 自动降级为普通实色胶囊。
 *
 * [dark] 为终端页纯黑背景的专属配色开关（避免黑底上出现一块亮色胶囊）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbStatusPill(dark: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiMode = LocalUiMode.current
    val devices by UsbHost.devices.collectAsStateWithLifecycle()
    val pillBlurEnabled = LocalUsbPillBlur.current
    var expanded by remember { mutableStateOf(false) }
    var infoDevice by remember { mutableStateOf<UsbDeviceInfo?>(null) }

    // 挂载即枚举一次，确保进入页面时立刻反映已接入的设备（自动访问）
    LaunchedEffect(Unit) { UsbHost.refresh(context) }

    val connected = devices.isNotEmpty()
    val granted = devices.any { it.granted }
    val statusText = stringResource(
        if (connected) R.string.usb_status_connected_device else R.string.usb_status_none,
    )

    // 颜色随主题设置的取色走：白 / 黑 / 纯黑 / 动态取色全部由 colorScheme 决定
    val baseContainer = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surfaceVariant
        UiMode.Material -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val baseContent = when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
    }
    val containerColor = if (dark) Color(0xFF2A2A2A) else baseContainer
    val contentColor = if (dark) Color(0xFFE6E6E6) else baseContent

    val dotColor = when {
        !connected -> if (dark) Color(0xFF9AA0A6) else baseContent.copy(alpha = 0.45f)
        granted -> Color(0xFF4CAF50)
        else -> Color(0xFFFFB300)
    }

    // 液态玻璃的「背景色」：跟随主题取色的真实页面背景（浅色白 / 深色黑 / 纯黑 /
    // 动态取色全部由 colorScheme 决定），与 MainActivity 页面级 backdrop 里
    // drawRect(surfaceColor) 用的是同一套。之前这里用 surfaceVariant.copy(0.32f)
    // 且采样层 alpha(0f) 是空的，backdrop 里没有任何实色或内容，玻璃采样后就是
    // 一片空灰 —— 这正是「没取到背景颜色」的根因。
    val glassBase = if (dark) Color(0xFF101010) else when (uiMode) {
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
        UiMode.Material -> MaterialTheme.colorScheme.surface
    }

    val pillShape = RoundedCornerShape(50)
    // 胶囊玻璃是「效果 = 渐变模糊」体系下的子选项：只有「模糊开启 + 效果选渐变 +
    // 胶囊模糊开关」三者同时满足才生效（与主题设置页里该选项的显隐条件严格一致）。
    val blurActive = LocalEnableBlur.current && LocalBlurStyle.current == "gradient"
    // RuntimeShader 需要 API 33；低版本不触碰 blur 链路，避免 NoClassDefFoundError
    val glassEnabled = pillBlurEnabled && blurActive &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // 液态玻璃的模糊源：先铺一层跟随主题取色的不透明页面背景色，再叠加被捕获层
    // 的真实内容（见下方采样层）。这样 backdrop 里既有「背景颜色」、也有可供
    // blur / lens 折射的内容纹理，玻璃才能真正取到背景，而不是一片空灰。
    val glassBackdrop = if (glassEnabled) {
        rememberLayerBackdrop {
            drawRect(glassBase)
            drawContent()
        }
    } else null

    val openInfo: (UsbDeviceInfo) -> Unit = {
        expanded = false
        infoDevice = it
    }

    Box(modifier = modifier) {
        // 采样层：铺真实页面背景色、与胶囊同尺寸，注册为液态玻璃的模糊源。
        // 关键：不能再 alpha(0f) —— 那样 layerBackdrop 捕获到的是完全透明的空层，
        // drawContent() 什么都没有，玻璃就取不到背景（MainActivity 对 pager backdrop
        // 的注释也写明「抓到空层会糊成一片」）。上层胶囊的半透明玻璃会盖住这一层，
        // 只透出被 blur / lens 折射后的背景色与内容纹理。
        if (glassEnabled && glassBackdrop != null) {
            Box(
                modifier = Modifier
                    .background(glassBase, pillShape)
                    .layerBackdrop(glassBackdrop),
            ) {
                when (uiMode) {
                    UiMode.Miuix -> PillContentMiuix(statusText, dotColor, contentColor)
                    UiMode.Material -> PillContentMaterial(statusText, dotColor, contentColor)
                }
            }
        }

        // 图二玻璃圆圈三要素：柔和投影(dropShadow) + 白色高光环(highlight) + 半透明玻璃底，
        // 再加 lens 边缘折射。Surface 本身设 Transparent，避免把 glass 层盖成实心色。
        val surfaceModifier = Modifier
            .then(
                if (glassEnabled && glassBackdrop != null) {
                    Modifier
                        .dropShadow(
                            shape = pillShape,
                            shadow = Shadow(
                                radius = 9.dp,
                                color = Color.Black,
                                alpha = if (dark) 0.30f else 0.16f,
                            ),
                        )
                        .drawBackdrop(
                            backdrop = glassBackdrop,
                            shape = { pillShape },
                            effects = {
                                padding = maxOf(padding, 26.dp.toPx())
                                vibrancy()
                                blur(6.dp.toPx(), 6.dp.toPx())
                                lens(
                                    refractionHeight = 20.dp.toPx(),
                                    refractionAmount = 20.dp.toPx(),
                                    depthEffect = true,
                                )
                            },
                            highlight = { PillGlassHighlight },
                            // 玻璃表面只留一层极薄 tint，别再把刚取到的背景色盖成灰
                            onDrawSurface = { drawRect(glassBase.copy(alpha = 0.12f)) },
                        )
                } else {
                    Modifier
                },
            )

        when (uiMode) {
            UiMode.Miuix -> MiuixSurface(
                onClick = { expanded = !expanded },
                modifier = surfaceModifier,
                shape = pillShape,
                color = if (glassEnabled) Color.Transparent else containerColor,
                contentColor = contentColor,
            ) {
                PillContentMiuix(statusText, dotColor, contentColor)
            }

            UiMode.Material -> M3Surface(
                onClick = { expanded = !expanded },
                modifier = surfaceModifier,
                shape = pillShape,
                color = if (glassEnabled) Color.Transparent else containerColor,
                contentColor = contentColor,
                tonalElevation = 0.dp,
            ) {
                PillContentMaterial(statusText, dotColor, contentColor)
            }
        }

        // 下拉挂在同一个 Box 里：Miuix 的 OverlayDropdownPopup 以父布局边界为锚点
        when (uiMode) {
            UiMode.Miuix -> MiuixDeviceDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                devices = devices,
                onDeviceClick = openInfo,
                onRequest = { UsbHost.requestPermission(context, it.device) },
                onRefresh = { UsbHost.refresh(context) },
            )

            UiMode.Material -> MaterialDeviceDropdown(
                expanded = expanded,
                onDismiss = { expanded = false },
                devices = devices,
                onDeviceClick = openInfo,
                onRequest = { UsbHost.requestPermission(context, it.device) },
                onRefresh = { UsbHost.refresh(context) },
            )
        }
    }

    infoDevice?.let { info ->
        UsbDeviceInfoDialog(info = info, onDismiss = { infoDevice = null })
    }
}

@Composable
private fun PillContentMiuix(text: String, dotColor: Color, contentColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        MiuixText(text, color = contentColor, fontSize = 13.sp, maxLines = 1)
        MiuixIcon(
            imageVector = Icons.Rounded.ArrowDropUp,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PillContentMaterial(text: String, dotColor: Color, contentColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        M3Text(text, color = contentColor, fontSize = 13.sp, maxLines = 1)
        Icon(
            Icons.Rounded.ArrowDropUp,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 按连接模式给出用于下拉项的短类型名 */
private fun modeShortType(mode: UsbMode): String = when (mode) {
    UsbMode.ADB -> "ADB"
    UsbMode.FASTBOOT -> "Fastboot"
    UsbMode.EDL, UsbMode.DLOAD -> "EDL"
    UsbMode.MTK_BROM -> "MTK"
    else -> "USB" // QUALCOMM / XIAOMI / OPPO / ONEPLUS / ANDROID_BRAND / NONE
}

/** 下拉里一条设备项的文案：类型 + 小写 VID:PID，如「ADB 22d9:2769」 */
private fun deviceItemText(info: UsbDeviceInfo): String =
    "${modeShortType(info.mode)} ${info.vidPid.lowercase()}"

/**
 * MIUI 风格下拉：直接用 Miuix 的 [OverlayDropdownPopup]（设置页下拉同款组件）。
 * 两组：设备（含「请求 USB 权限」动作）+ 「刷新列表」，与 MIUI 系统的分组下拉一致。
 */
@Composable
private fun MiuixDeviceDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    devices: List<UsbDeviceInfo>,
    onDeviceClick: (UsbDeviceInfo) -> Unit,
    onRequest: (UsbDeviceInfo) -> Unit,
    onRefresh: () -> Unit,
) {
    val noDeviceText = stringResource(R.string.usb_menu_no_device)
    val requestText = stringResource(R.string.usb_request_access)
    val refreshText = stringResource(R.string.usb_refresh_list)

    val deviceItems = buildList<DropdownItem> {
        if (devices.isEmpty()) {
            add(DropdownItem(text = noDeviceText, enabled = false))
        } else {
            devices.forEach { info ->
                add(
                    DropdownItem(
                        text = deviceItemText(info),
                        onClick = { onDeviceClick(info) },
                    )
                )
                if (!info.granted) {
                    add(DropdownItem(text = requestText, onClick = { onRequest(info) }))
                }
            }
        }
    }
    val refreshItems = listOf(DropdownItem(text = refreshText, onClick = onRefresh))

    OverlayDropdownPopup(
        entries = listOf(
            DropdownEntry(items = deviceItems),
            DropdownEntry(items = refreshItems),
        ),
        show = expanded,
        onDismiss = onDismiss,
        onDismissFinished = {},
        maxHeight = null,
        dropdownColors = DropdownDefaults.dropdownColors(),
        renderInRootScaffold = true,
        collapseOnSelection = true,
    )
}

/** Material3 风格下拉：沿用 M3 DropdownMenu / DropdownMenuItem */
@Composable
private fun MaterialDeviceDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    devices: List<UsbDeviceInfo>,
    onDeviceClick: (UsbDeviceInfo) -> Unit,
    onRequest: (UsbDeviceInfo) -> Unit,
    onRefresh: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(240.dp),
    ) {
        if (devices.isEmpty()) {
            DropdownMenuItem(
                text = { M3Text(stringResource(R.string.usb_menu_no_device)) },
                onClick = onDismiss,
                enabled = false,
            )
        } else {
            devices.forEach { info ->
                DropdownMenuItem(
                    text = { M3Text(deviceItemText(info)) },
                    trailingIcon = {
                        if (info.granted) {
                            M3Text(
                                stringResource(R.string.usb_granted),
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                            )
                        } else {
                            M3TextButton(onClick = { onRequest(info) }) {
                                M3Text(stringResource(R.string.usb_request))
                            }
                        }
                    },
                    onClick = { onDeviceClick(info) },
                )
            }
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { M3Text(stringResource(R.string.usb_refresh_list)) },
            onClick = {
                onRefresh()
                onDismiss()
            },
        )
    }
}
