package com.android.ozjflash.ui.component.material

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.ozjflash.ui.component.GlassBackButton

/**
 * Material3 二级页通用返回按钮：圆形液态玻璃高光圆钮（与 Miuix 侧 [GlassBackButton] 同源）。
 * 配色页 / 关于页的 LargeFlexibleTopAppBar 都复用此组件，替换后自动生效。
 */
@Composable
fun TopBarBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    GlassBackButton(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        onClick = onClick,
        modifier = modifier.padding(horizontal = 6.dp),
        contentDescription = contentDescription,
    )
}
