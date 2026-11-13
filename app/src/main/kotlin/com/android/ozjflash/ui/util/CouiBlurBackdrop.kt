package com.android.ozjflash.ui.util

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.android.ozjflash.ui.theme.LocalBlurStyle
import com.android.ozjflash.ui.theme.LocalEnableBlur
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop

/**
 * COUI-Kit per-page 背景采样源，服务于顶栏「透明渐变」模糊（[BlurredBar]）。
 *
 * 两套采样体系并存、互不通用：
 * - Miuix `LayerBackdrop` → 胶囊 / 圆钮真折射 + 「默认」高斯模糊（[rememberBlurBackdrop]）；
 * - COUI  `CouiBackdrop`  → 顶栏 `drawCouiGradientBackdrop` 原厂渐进透明高斯。
 *
 * COUI 约束「一个源只挂一个内容节点、消费者在源子树之外」：因此**不做全局单源**
 * （全局单源会让顶栏落进源子树造成自采样，且 HorizontalPager 一次预组合 4 页共用
 * 一份坐标会互相覆盖）。正确装配：每个页面自己 [rememberCouiBlurBackdrop]，把源挂到
 * 正文节点（[Modifier.couiBlurSource]），顶栏 [BlurredBar] 作为源外消费者显式接收同一实例。
 *
 * 仅当「模糊开启 + 效果 = 透明渐变(gradient) + API 33+」时激活；否则返回 null，
 * `couiBlurSource` 退化为无操作，零采样开销。RuntimeShader 需 API 33。
 */
@Composable
fun rememberCouiBlurBackdrop(): CouiBackdrop? {
    // rememberCouiBackdrop 必须无条件调用（Compose 规则），只在满足门槛时对外暴露
    val backdrop = rememberCouiBackdrop()
    val active = LocalEnableBlur.current &&
        LocalBlurStyle.current == "gradient" &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    return if (active) backdrop else null
}

/** 把 COUI 采样源挂到页面正文节点；backdrop 为 null 时无操作。 */
fun Modifier.couiBlurSource(backdrop: CouiBackdrop?): Modifier =
    if (backdrop != null) this.couiBackdrop(backdrop) else this

/**
 * 当前页面 COUI 采样源，由页面注入 [BlurredBar]，供顶栏作为「源外消费者」读取。
 * 与页面正文 [Modifier.couiBlurSource] 是**同一个实例**，不新增采样源、不自采样。
 */
val LocalCouiBlurBackdrop = staticCompositionLocalOf<CouiBackdrop?> { null }
