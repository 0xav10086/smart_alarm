#!/bin/bash

PACKAGE="io.github.av10086.smartalarm"

# 清空旧日志（确保后续日志是全新的）
adb logcat -c

# 启动应用
adb shell am start -n $PACKAGE/.MainActivity

# 等待足够时间让崩溃日志完全写入（闪退可能需要多一点时间）
sleep 3

# 生成文件名
filename=$(date +"%Y-%m-%d_%H-%M-%S").log

# 抓取所有缓冲区的最近500条日志，并过滤崩溃关键词和包名
adb logcat -b all -d -t 500 | grep -E "AndroidRuntime|FATAL EXCEPTION|CRASH|$PACKAGE" > "$filename"

echo "日志已保存到: $(pwd)/$filename"