#!/system/bin/sh

# ==============================================================================
# Gadgetbridge Force Sync Helper
# ==============================================================================

# 该脚本通过重启GB进程后模拟用户点击来同步数据，但是GB并不会重新连接设备，但此脚本提供了一个很好的解决方案，之后将通过代码注入的方式来同步数据
# 2026年2月18日之后的代码，走 Zygisk 路线 (C++ 注入)：
#1. 创建 C++ 项目：编写 main.cpp，继承 zygisk::ModuleBase。
#2. Hook 进程：在 onAppSpecialize 中检测包名是否为 nodomain.freeyourgadget.gadgetbridge。
#3. JNI 调用： 使用 JNI 获取 GBApplication 类 -> 获取 deviceService 实例 -> 调用 onFetchRecordedData 方法。
#4. 编译：使用 CMake 和 NDK 编译出 libsmart_alarm.so。
#5. 打包：将 .so 放入模块的 Lsposed 目录，并在 module.prop 中添加 Lsposed=true。
# 具体代码网址为以下：
# https://codeberg.org/Freeyourgadget/Gadgetbridge/src/commit/bf46baa5dea2ddf1eaceb0a813109cc67403971a/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/adapter/GBDeviceAdapterv2.java
# 涉及的代码主要字段为以下：
# GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);
# showTransientSnackbar(R.string.busy_task_fetch_activity_data);

MODDIR=${0%/*}
[ -z "$MODDIR" ] && MODDIR="."

PKG="nodomain.freeyourgadget.gadgetbridge"

# 0. Define Logger
log_info() {
    echo "[SYNC] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 1. Setup Environment for db.sh
export GB_DB_ORIG="/data/data/$PKG/databases/Gadgetbridge"
export TEMP_DB="/dev/gb_snap_sync.db"

# Locate SQLite
if [ -x "$MODDIR/sqlite3" ]; then
    export SQLITE="$MODDIR/sqlite3"
elif [ -x "/data/local/tmp/sqlite3" ]; then
    export SQLITE="/data/local/tmp/sqlite3"
else
    log_info "❌ Error: sqlite3 binary not found"
    exit 1
fi

# Source db.sh
if [ -f "$MODDIR/db.sh" ]; then
    source "$MODDIR/db.sh"
else
    log_info "❌ Error: db.sh not found"
    exit 1
fi

# 2. Check if installed
if ! pm list packages | grep -q "$PKG"; then
    log_info "❌ Gadgetbridge 未安装 ($PKG)"
    exit 1
fi

log_info "🔄 触发 Zygisk 注入同步..."

# 3. Trigger Zygisk Module via File Signal
# The injected C++ code monitors this file.
TRIGGER_FILE="/data/data/$PKG/cache/smart_alarm_trigger"

log_info "⚡ 创建触发信号: $TRIGGER_FILE"
touch "$TRIGGER_FILE"
# Ensure the app can read/delete it (though root created it, cache is usually writable)
chmod 666 "$TRIGGER_FILE"

# 5. Wait for sync
log_info "⏳ 等待 20 秒让数据同步..."
sleep 20

# 6. Verify Data using db.sh
log_info "🔍 验证数据同步结果..."

# Update snapshot
update_db_snapshot
if [ $? -ne 0 ]; then
    log_info "❌ 数据库快照更新失败"
    exit 1
fi

NOW=$(date +%s)

# --- Heart Rate Analysis ---
log_info "❤️ [心率数据] 最近 5 条记录:"
# TIMESTAMP is seconds in XIAOMI_ACTIVITY_SAMPLE
HR_QUERY="SELECT TIMESTAMP, HEART_RATE FROM XIAOMI_ACTIVITY_SAMPLE WHERE HEART_RATE > 0 ORDER BY TIMESTAMP DESC LIMIT 5;"
HR_DATA=$($SQLITE "$TEMP_DB" "$HR_QUERY")

if [ -z "$HR_DATA" ]; then
    log_info "   ⚠️ 未找到心率数据"
else
    echo "$HR_DATA" | while IFS='|' read -r ts val; do
        if [ -n "$ts" ]; then
            date_str=$(date -d @$ts '+%Y-%m-%d %H:%M:%S')
            diff=$((NOW - ts))
            log_info "   🕒 $date_str ($diff 秒前) => $val BPM"
        fi
    done
fi

# --- Sleep Data Analysis ---
log_info "🛏️ [睡眠数据] 最近 5 条记录:"
# TIMESTAMP is milliseconds in XIAOMI_SLEEP_TIME_SAMPLE
SLEEP_QUERY="SELECT TIMESTAMP/1000, WAKEUP_TIME/1000 FROM XIAOMI_SLEEP_TIME_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 5;"
SLEEP_DATA=$($SQLITE "$TEMP_DB" "$SLEEP_QUERY")

if [ -z "$SLEEP_DATA" ]; then
    log_info "   ⚠️ 未找到睡眠数据"
else
    echo "$SLEEP_DATA" | while IFS='|' read -r ts wake; do
        if [ -n "$ts" ]; then
            date_str=$(date -d @$ts '+%Y-%m-%d %H:%M:%S')
            diff=$((NOW - ts))
            
            wake_msg="😴 仍在睡眠中"
            if [ "$wake" -gt 0 ]; then
                 wake_str=$(date -d @$wake '+%Y-%m-%d %H:%M:%S')
                 wake_msg="🌅 醒来于 $wake_str"
            fi
            
            log_info "   🕒 $date_str ($diff 秒前) => $wake_msg"
        fi
    done
fi

log_info "✅ 同步检查完成"