# OZJFLASH

基于 Compose UI 框架，内置**终端**并嵌入**免 root 刷写功能**的安卓工具箱。

> 本项目由两部分来源整合而成：
> 1. `OZJFLASH.zip` 提供的 Compose UI 框架（包名 `com.android.ozjflash`）；
> 2. 对 `MOFLASH.apk`（同系列发布包）逆向分析后提取的刷写引擎，以原生库形式内置。

## 架构

```
MainActivity (Compose)
 ├─ 终端 Tab  → TerminalScreen → TerminalEngine
 │               ├─ PROCESS 模式（默认）：ProcessBuilder 拉起本地 shell，免 root
 │               └─ PTY 模式：libtermux.so (com.termux.terminal.JNI) 提供真实 PTY
 └─ 刷写 Tab  → FlashScreen → 通过终端执行内置 fastboot / adb 命令

NativeLibs.prepare() 把打包的改名可执行二进制释放到 files/bin 并注入终端 PATH：
  libadb.so→adb  libfastboot.so→fastboot  libbash.so→bash
  lib7zz.so→7z   libgrep.so→grep  libsimg2img.so→simg2img  liblpmake.so→lpmake

JNI 桥接（com.Android.MoFlash.data.*，匹配 lib*.so 导出符号）：
  NativeCmd / FdPass / MiUnlockNative / MtkArgs / QdlArgs / CmdGen / Secrets
USB 通信底座：UsbHostManager（android.hardware.usb）
```

## 内置原生库（app/src/main/jniLibs/arm64-v8a）

| 库 | 作用 |
|----|------|
| libtermux.so | Termux 终端 JNI（PTY） |
| libfdpass.so | fd 传递 |
| libnativecmd.so | fastboot 协议封装（26 个 JNI 方法） |
| libqdl.so / libqdlargs.so | 高通 EDL / 9008 |
| libmtkcmd.so | MediaTek 协议 |
| libmiunlock.so | 小米 BL 解锁 |
| libksud.so | KernelSU |
| libpayload-dumper-go.so | OTA payload.bin 解包 |
| libadb / libfastboot / libbash / lib7zz / libgrep / libsimg2img / liblpmake | 改名可执行二进制 |

## 构建

```bash
./gradlew assembleDebug
```

要求：Android SDK（compileSdk 37）、JDK 17+、网络可访问 Google/Maven Central。
仅支持 `arm64-v8a`（minSdk 26）。

## 免责声明

本工具可被用于 bootloader 锁 / FRP 锁 / AVB 验证的绕过，误操作会导致设备变砖、
保修失效。请仅用于**自有设备的合法维修、学习与研究**，由此产生的一切后果由使用者自行承担。
