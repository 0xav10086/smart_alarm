#!/system/bin/sh

# ==============================================================================
# Smart Alarm Integration Test
# ==============================================================================

MODDIR=${0%/*}
[ -z "$MODDIR" ] && MODDIR="."

# [Fix] 如果在子目录(test_module)运行，自动定位到父目录查找组件
if [ ! -f "$MODDIR/time.sh" ] && [ -f "$MODDIR/../time.sh" ]; then
    MODDIR="$MODDIR/.."
fi

# 1. Setup Mock Environment
TEST_DIR="/data/local/tmp/smart_alarm_test_$$"
mkdir -p "$TEST_DIR"

# Override Paths for Testing
export CONFIG_FILE="$TEST_DIR/test_config.conf"
export TEMP_DB="$TEST_DIR/test_gb.db"

# Mock Logging to stdout
log_info() {
    echo "[TEST] $(date '+%H:%M:%S') - $1"
}

# Mock rotate_log to avoid error in alarm.sh
rotate_log() {
    echo "[TEST] Log rotation requested (mocked)."
}

# 2. Load Components
log_info "Loading modules from $MODDIR..."
source "$MODDIR/time.sh"
source "$MODDIR/db.sh"
source "$MODDIR/alarm.sh"
source "$MODDIR/config.sh"

# Locate SQLite
if [ -x "$MODDIR/sqlite3" ]; then
    SQLITE="$MODDIR/sqlite3"
elif [ -x "/data/local/tmp/sqlite3" ]; then
    SQLITE="/data/local/tmp/sqlite3"
else
    log_info "ERROR: sqlite3 binary not found!"
    exit 1
fi

# 3. Create Fake Database
log_info "Creating Fake Database at $TEMP_DB..."
rm -f "$TEMP_DB"
$SQLITE "$TEMP_DB" "CREATE TABLE XIAOMI_SLEEP_TIME_SAMPLE (TIMESTAMP INTEGER, WAKEUP_TIME INTEGER);"
$SQLITE "$TEMP_DB" "CREATE TABLE XIAOMI_SLEEP_STAGE_SAMPLE (TIMESTAMP INTEGER, STAGE INTEGER);"
$SQLITE "$TEMP_DB" "CREATE TABLE XIAOMI_ACTIVITY_SAMPLE (TIMESTAMP INTEGER, RAW_INTENSITY INTEGER, HEART_RATE INTEGER);"

# Insert Data: Sleep started 9 hours ago (ensure sleep duration is met)
NOW_TS=$(date +%s)
SLEEP_START=$((NOW_TS - 9*3600))
# DB uses milliseconds
$SQLITE "$TEMP_DB" "INSERT INTO XIAOMI_SLEEP_TIME_SAMPLE VALUES (${SLEEP_START}000, 0);"

# Insert Data: Light Sleep (Stage 3) 1 minute ago
STAGE_TS=$((NOW_TS - 60))
$SQLITE "$TEMP_DB" "INSERT INTO XIAOMI_SLEEP_STAGE_SAMPLE VALUES (${STAGE_TS}000, 3);"

log_info "Fake DB Data Injected:"
log_info "  - Sleep Start: $(date -d @$SLEEP_START '+%Y-%m-%d %H:%M:%S') (9 hours ago)"
log_info "  - Last Stage: Light Sleep (Stage 3)"

# 4. Create Fake Config
# Calculate Current + 1 Minute (Handle hour/day rollover)
CUR_H=$(date +%H)
CUR_M=$(date +%M)
NEXT_HM=$(awk -v h=$CUR_H -v m=$CUR_M 'BEGIN {m++; if(m>=60){m=0; h++}; if(h>=24){h=0}; printf "%02d:%02d", h, m}')
log_info "Creating Fake Config with Expected Wakeup: $NEXT_HM (Current + 1min)"

cat <<EOF > "$CONFIG_FILE"
EXPECTED_BEDTIME="22:00"
EXPECTED_WAKEUP="$NEXT_HM"
EXPECTED_SLEEP_DURATION="8"
RINGTONE_PATH=""
RINGTONE_DURATION="5"
RINGTONE_INTERVAL="10"
RINGTONE_COUNT="1"
GENTLE_WAKE="true"
GENTLE_WAKE_WINDOW="20"
HR_THRESHOLD="60"
INTENSITY_THRESHOLD="60"
DETECTION_WINDOW="15"
LOG_RETENTION="3"
POLLING_MODE="dynamic"
CHECK_INTERVAL="10"
EOF

load_config
log_info "Config Loaded. RINGTONE_DURATION set to 5s for test."

# 5. No Wait (Immediate Check)

# 6. Run Logic Check
log_info "Running Alarm Logic Check..."

NOW_TS=$(date +%s)
EXPECTED_WAKEUP_TS=$(get_today_timestamp "$EXPECTED_WAKEUP")

SHOULD_RING=0
REASON=""

# Check Deadline (Since we set Expected Wakeup to NOW, this should be true or very close)
if [ $NOW_TS -ge $EXPECTED_WAKEUP_TS ]; then
    SHOULD_RING=1; REASON="Deadline Reached (Test)"
fi

# Check Duration (We slept 9 hours, expected 8)
if [ $SHOULD_RING -eq 0 ]; then
    ACTUAL_BEDTIME=$(get_actual_bedtime)
    if [ -n "$ACTUAL_BEDTIME" ]; then
         SLEEP_DUR=$((NOW_TS - ACTUAL_BEDTIME))
         EXP_DUR_SEC=$(awk "BEGIN {print int($EXPECTED_SLEEP_DURATION * 3600)}")
         if [ $SLEEP_DUR -ge $EXP_DUR_SEC ]; then
             SHOULD_RING=1; REASON="Sleep Duration Met (Test)"
         fi
    fi
fi

if [ $SHOULD_RING -eq 1 ]; then
    log_info "DECISION: RING ALARM! Reason: $REASON"
    trigger_alarm "$REASON"
else
    log_info "DECISION: DO NOT RING. (Something went wrong with the test setup)"
fi

# Cleanup
rm -rf "$TEST_DIR"
log_info "Test Finished."