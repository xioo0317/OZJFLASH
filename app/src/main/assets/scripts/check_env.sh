#!/system/bin/sh
# OZJFLASH 内置工具自检脚本
#
# 在 App 的内置终端里执行即可（无需 root）：
#     sh check_env.sh
# 所有工具由 NativeLibs.prepare() 释放到私有 bin 目录并注入 PATH。

bin_path=$(command -v adb 2>/dev/null)
bin_dir=${bin_path%/*}

check_tool() {
    if command -v "$1" >/dev/null 2>&1; then
        p=$(command -v "$1")
        printf '\033[32m[√] %-20s %s\033[0m\n' "$1" "$p"
        return 0
    else
        printf '\033[31m[×] %-20s 缺失\033[0m\n' "$1"
        return 1
    fi
}

sh_path=$(command -v sh)
bash_path=$(command -v bash)
clear

missing=0
printf '\n\n'
if [ "$sh_path" != "" ] && echo "$sh_path" | grep -q "ozjflash"; then
    printf '运行环境：📱 OZJFLASH 内置终端\n'
elif [ "$bash_path" != "" ] && echo "$bash_path" | grep -q "com.termux"; then
    printf '运行环境：📱 Termux\n'
elif [ "$sh_path" = "/system/bin/sh" ]; then
    printf '运行环境：📱 Android\n'
elif [ "$bash_path" = "/usr/bin/bash" ]; then
    printf '运行环境：💻 Linux\n'
else
    printf '运行环境：❗未知\n'
fi
printf '工具目录：%s\n' "${bin_dir:-未注入}"
printf 'PATH：%s\n' "${PATH%%:*}"

printf '\n基本工具（必要）\n'
for t in adb fastboot sh bash tadb; do check_tool "$t" || missing=$((missing + 1)); done

printf '\n分区与镜像（可选）\n'
for t in lpmake lpunpack simg2img img2simg; do check_tool "$t"; done

printf '\n文件系统工具（可选）\n'
for t in mke2fs e2fsdroid make_f2fs make_f2fs_casefold sload_f2fs; do check_tool "$t"; done

printf '\n解包工具（可选）\n'
for t in payload-dumper-go magiskboot gzip tar 7z; do check_tool "$t"; done

printf '\n9008 / EDL（可选）\n'
for t in edl qdl; do check_tool "$t"; done

printf '\n其它（可选）\n'
for t in sqlite3 etc1tool hprof-conv libksud; do check_tool "$t"; done

printf '\n版本信息\n'
printf 'adb      : %s\n' "$(adb version 2>&1 | head -n1)"
printf 'fastboot : %s\n' "$(fastboot --version 2>&1 | head -n1)"

printf '\n'
if [ "$missing" -eq 0 ]; then
    printf '\033[32m必要工具齐备，可以直接下发 fastboot / adb 指令。\033[0m\n'
else
    printf '\033[33m有 %s 个必要工具缺失，请检查 App 是否完成资源释放。\033[0m\n' "$missing"
fi
