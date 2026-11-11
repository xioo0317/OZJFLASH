package com.android.ozjflash.ui.util

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.ozjflash.ui.theme.LocalUiRevamp
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop

/**
 * 界面换新 (Beta)：为「单个页面」创建 COUI 背景采样源。
 *
 * 两套采样体系互不通用、并存使用：
 * - Miuix `LayerBackdrop` → 玻璃胶囊 / 圆钮的 `drawBackdrop` 真折射（[rememberBlurBackdrop]）；
 * - COUI  `CouiBackdrop`  → 顶栏 [BlurredBar] 的 COUI 渐进高斯模糊。
 *
 * COUI 要求「一个源只挂到一个内容节点，消费者位于源子树之外」。因此这里刻意
 * **不做全局单源、也不在外层 Box 上挂源**：全局单源会让顶栏落进源子树造成自采样，
 * 且多页面（HorizontalPager 一次预组合 4 页）共用一份坐标会互相覆盖。
 *
 * 正确装配（与 COUI 官方文档一致）：每个页面自己 `rememberRevampBackdrop()`，
 * 把源挂到正文节点（[Modifier.revampSource]），顶栏 `BlurredBar` 作为源外消费者
 * 显式接收同一个实例。
 *
 * 未调用 `Coui.initialize` 是刻意的：底层采样 / 模糊 Modifier 只读传入的 backdrop，
 * 不依赖主题策略；`Coui.defaultStyle` 有 `NativeCouiStyle` 兜底，`isDark` 回退系统主题。
 * COUI 渐进模糊依赖 RuntimeShader（API 33），低版本返回 null，顶栏自动回退 Miuix 模糊。
 */
@Composable
fun rememberRevampBackdrop(): CouiBackdrop? {
    // rememberCouiBackdrop 必须无条件调用（Compose 规则），只在满足门槛时对外暴露
    val backdrop = rememberCouiBackdrop()
    val active = LocalUiRevamp.current && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    return if (active) backdrop else null
}

/** 把 COUI 采样源挂到页面正文节点；backdrop 为 null 时是无操作。 */
fun Modifier.revampSource(backdrop: CouiBackdrop?): Modifier =
    if (backdrop != null) this.couiBackdrop(backdrop) else this
