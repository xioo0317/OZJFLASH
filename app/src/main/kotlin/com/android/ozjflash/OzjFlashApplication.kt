package com.android.ozjflash

import android.app.Application

/**
 * OZJFLASH 应用入口。
 *
 * 仅持有全局 Context（供设置存储、工具路径等使用），
 * 不含任何 root 行为 —— 本应用完全运行在普通应用沙箱内。
 */
class OzjFlashApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        lateinit var appContext: Application
            private set
    }
}

/** 全局 Application 句柄，等价于 KernelSU 里的 ksuApp */
val ozjApp: Application get() = OzjFlashApplication.appContext
