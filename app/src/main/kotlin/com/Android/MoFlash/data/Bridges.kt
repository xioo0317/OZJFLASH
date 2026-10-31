package com.Android.MoFlash.data

/**
 * JNI 桥接类集合。包名 / 类名 / 方法名精确匹配 MOFLASH.apk 中 lib*.so 导出的
 * Java_com_Android_MoFlash_data_* 符号，因此 System.loadLibrary 后即可被解析。
 *
 * 注意：external fun 的「参数类型」依据 APK 逆向推断（短格式 JNI 符号不含类型信息）。
 * 设备端首次调用前应校验签名；若不确定，建议优先走「终端 + 内置二进制」路径
 * （见 NativeLibs / FlashActions），避免参数不匹配引发 native 层异常。
 */

class NativeCmd {
    companion object { init { try { System.loadLibrary("nativecmd") } catch (_: Throwable) {} } }

    external fun nDevices(): String?
    external fun nGetvarAll(fd: Int): String?
    external fun nGetvarSuperSize(fd: Int): String?
    external fun nGetvarPartitionType(fd: Int): String?
    external fun nGetvarPartitionSize(fd: Int): String?
    external fun nGetvarCurrentSlot(fd: Int): String?
    external fun nGetvarIsUserspace(fd: Int): String?
    external fun nRebootFastboot(fd: Int)
    external fun nRebootBootloader(fd: Int)
    external fun nRebootMode(fd: Int, mode: String?)
    external fun nSetActive(fd: Int, slot: String?)
    external fun nFlash(fd: Int, partition: String?, file: String?)
    external fun nFlashPart(fd: Int, partition: String?, file: String?)
    external fun nErasePart(fd: Int, partition: String?)
    external fun nEraseUserdata(fd: Int)
    external fun nEraseMetadata(fd: Int)
    external fun nWipeFlag(fd: Int)
    external fun nWipeSuper(fd: Int)
    external fun nCreateLogical(fd: Int, partition: String?, size: Long)
    external fun nDeleteLogical(fd: Int, partition: String?)
    external fun nSlotSuffixes(fd: Int): String?
    external fun nSlots(fd: Int): String?
    external fun nSnapshotCancel(fd: Int)
    external fun nBootImage(fd: Int, file: String?)
    external fun nDisableVerityFlashVbmeta(fd: Int, file: String?)
    external fun nHelp(): String?
}

class FdPass {
    companion object { init { try { System.loadLibrary("fdpass") } catch (_: Throwable) {} } }
    external fun sendFd(fd: Int, targetPid: Int, targetFd: Int)
    external fun serveFdLoop(fd: Int)
    external fun stopServeLoop()
}

class MiUnlockNative {
    companion object { init { try { System.loadLibrary("miunlock") } catch (_: Throwable) {} } }
    external fun nBaseChina(): String?
    external fun nBaseEurope(): String?
    external fun nBaseGlobal(): String?
    external fun nBaseIndia(): String?
    external fun nBaseRussia(): String?
    external fun nDefaultIv(): String?
    external fun nHmacKeyHex(): String?
    external fun nLoginUrl(): String?
    external fun nPathAhaUnlockV3(): String?
    external fun nPathDeviceClearV3(): String?
    external fun nPathNonceV2(): String?
    external fun nPathUserinfoV3(): String?
}

class MtkArgs {
    companion object { init { try { System.loadLibrary("mtkcmd") } catch (_: Throwable) {} } }
    external fun nWithDaPrefix(prefix: String?): String?
}

class QdlArgs {
    companion object { init { try { System.loadLibrary("qdlargs") } catch (_: Throwable) {} } }
    external fun nBuild(cfg: String?): String?
    external fun nBuildFullBase(cfg: String?): String?
    external fun nBuildQfil(cfg: String?): String?
    external fun nSubGetSlot(cfg: String?): String?
    external fun nSubRgpt(cfg: String?): String?
}

class CmdGen {
    companion object { init { try { System.loadLibrary("cmdgen") } catch (_: Throwable) {} } }
    external fun nativeLpmakeArgs(base: String?): String?
    external fun nFullLpmakeArgs(base: String?): String?
    external fun nKsudBootPatchArgs(base: String?): String?
}

class Secrets {
    companion object { init { try { System.loadLibrary("secrets") } catch (_: Throwable) {} } }
    external fun detectDebug(): Boolean
    external fun getSecret(key: String?): String?
    external fun verifySignature(data: String?): Boolean
}
