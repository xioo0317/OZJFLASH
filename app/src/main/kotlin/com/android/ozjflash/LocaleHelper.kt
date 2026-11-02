package com.android.ozjflash

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

/** 应用内支持的语言（system = 跟随系统）。与设置里的下拉项保持一致。 */
val SUPPORTED_LANGUAGES = listOf("system", "en", "zh-CN", "zh-TW", "ja", "es")

fun languageToLocale(code: String): Locale? = when (code) {
    "system", "" -> null
    "zh-CN" -> Locale.SIMPLIFIED_CHINESE
    "zh-TW" -> Locale.TRADITIONAL_CHINESE
    "ja" -> Locale.JAPANESE
    "es" -> Locale("es")
    else -> runCatching { Locale.forLanguageTag(code) }.getOrNull()
}

/** 按所选语言包装 Context；system 时返回原 Context（沿用系统 Locale）。 */
fun Context.wrapWithLanguage(code: String): Context {
    val locale = languageToLocale(code) ?: return this
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

/** 从任意 Context 向上找到所属 Activity，语言切换后用来 recreate()。 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
