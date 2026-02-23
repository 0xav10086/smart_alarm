#!/usr/bin/env bash

# 1. 定义文件名（在数据库中匹配 _display_name）
FILE_NAME="SA_default_ring.mp3"

# 2. 直接根据文件名查询 ID
echo "正在从媒体库检索文件 ID..."
QUERY_OUTPUT=$(adb shell "content query --uri content://media/external/audio/media --projection _id --where \"_display_name='$FILE_NAME'\"")

# 解析 ID
ID=$(echo "$QUERY_OUTPUT" | grep -o '_id=[0-9]*' | cut -d'=' -f2)

if [ -z "$ID" ]; then
    echo "错误：无法在手机媒体库中找到文件 $FILE_NAME"
    echo "请尝试手动运行一次媒体扫描："
    echo "adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d \"file:///storage/emulated/0/Alarms/$FILE_NAME\""
    exit 1
fi

CONTENT_URI="content://media/external/audio/media/$ID"
echo "匹配成功！ID: $ID"
echo "Content URI: $CONTENT_URI"

# 3. 设置闹钟
TARGET_HOUR=$(date -d "+1 minute" +"%H")
TARGET_MIN=$(date -d "+1 minute" +"%M")

echo "正在为 $TARGET_HOUR:$TARGET_MIN 设置闹钟..."

adb shell am start -a android.intent.action.SET_ALARM \
    --ei android.intent.extra.alarm.HOUR $TARGET_HOUR \
    --ei android.intent.extra.alarm.MINUTES $TARGET_MIN \
    --es android.intent.extra.alarm.RINGTONE "$CONTENT_URI" \
    --ez android.intent.extra.alarm.VIBRATE true \
    --ez android.intent.extra.alarm.SKIP_UI true