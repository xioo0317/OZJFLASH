package com.android.ozjflash

import android.app.Application
import android.content.Context

/**
 * OZJFLASH 应用入口。
 *
 * 仅持有全局 Context（供设置存储、工具路径等使用），
 * 不含任何 root 行为 —— 本应用完全运行在普通应用沙箱内。
 */
class OzjFlashApplication : Application() {

    override fun attachBaseContext(base: Context) {
        // 在首屏渲染前就把应用内语言应用到 Context（此时 ozjApp 尚未初始化，
        // 所以直接读 settings 这个 SharedPreferences，而不是走 SettingsRepository）。
        val prefs = base.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "system") ?: "system"
        super.attachBaseContext(base.wrapWithLanguage(lang))
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        // 崩溃栈写入 files/crash.log，便于事后定位（设置 → 崩溃日志 可查看）
        CrashLogger.install(this)
    }

    companion object {
        lateinit var appContext: Application
            private set
    }
}

/** 全局 Application 句柄，等价于 KernelSU 里的 ksuApp */
val ozjApp: Application get() = OzjFlashApplication.appContext
