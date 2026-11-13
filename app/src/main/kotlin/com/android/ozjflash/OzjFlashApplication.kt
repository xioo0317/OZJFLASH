package com.android.ozjflash

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.android.ozjflash.data.repository.SettingsRepositoryImpl
import com.android.ozjflash.data.repository.UiSettingsKeys
import org.lsposed.hiddenapibypass.HiddenApiBypass

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
        val prefs = base.getSharedPreferences(UiSettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(UiSettingsKeys.LANGUAGE, "system") ?: "system"
        super.attachBaseContext(base.wrapWithLanguage(lang))
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        // 崩溃栈写入 files/crash.log，便于事后定位（设置 → 崩溃日志 可查看）
        CrashLogger.install(this)

        // 预测性返回手势：Android 14+ 通过隐藏 API 全局开关，需在系统读取 ApplicationInfo 前设置，
        // 否则下次 Activity 重建才会生效（切换开关时由设置页主动 recreate）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                HiddenApiBypass.addHiddenApiExemptions(
                    "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback",
                )
                setEnableOnBackInvokedCallback(applicationInfo, SettingsRepositoryImpl().enablePredictiveBack)
            }
        }
    }

    companion object {
        lateinit var appContext: Application
            private set

        /**
         * 反射调用隐藏 API `ApplicationInfo.setEnableOnBackInvokedCallback`，
         * 全局开启 / 关闭系统预测性返回手势动画。照搬 KernelSU 实现。
         */
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val method = ApplicationInfo::class.java.getDeclaredMethod(
                    "setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType,
                )
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }
}

/** 全局 Application 句柄，等价于 KernelSU 里的 ksuApp */
val ozjApp: Application get() = OzjFlashApplication.appContext
