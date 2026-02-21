#!/system/bin/sh

# ==============================================================================
# Gadgetbridge DB Logic Unit Test
# Compares direct SQLite queries vs db.sh helper functions
# ==============================================================================

MODDIR=${0%/*}
[ -z "$MODDIR" ] && MODDIR="."

# Locate Module Root
if [ -f "$MODDIR/../db.sh" ]; then
    MODULE_ROOT="$MODDIR/.."
else
    MODULE_ROOT="$MODDIR"
fi

# --- Configuration ---
# Define variables required by db.sh
export GB_DB_ORIG="/data/data/nodomain.freeyourgadget.gadgetbridge/databases/Gadgetbridge"
export TEMP_DB="/dev/gb_snap_test.db"
export SQLITE="$MODULE_ROOT/sqlite3"
export INTENSITY_THRESHOLD=60

# Mock log_info (used by db.sh)
log_info() {
    echo "[DB_LOG] $1"
}

# Check Environment
if [ ! -x "$SQLITE" ]; then
    echo "❌ Error: sqlite3 binary not found at $SQLITE"
    exit 1
fi

if [ ! -f "$MODULE_ROOT/db.sh" ]; then
    echo "❌ Error: db.sh not found at $MODULE_ROOT/db.sh"
    exit 1
fi

# Source the script under test
source "$MODULE_ROOT/db.sh"

echo "=========================================="
echo "🧪 Gadgetbridge DB Logic Verification"
echo "=========================================="

# 1. Test Snapshot Creation
echo "[1] Testing update_db_snapshot()..."
rm -f "${TEMP_DB}"*

# Call function from db.sh
update_db_snapshot
if [ $? -eq 0 ] && [ -f "$TEMP_DB" ]; then
    echo "✅ Snapshot created successfully at $TEMP_DB"
else
    echo "❌ Snapshot creation failed!"
    exit 1
fi

echo "------------------------------------------"

# 2. Compare Bedtime (Last Sleep Time)
echo "[2] Comparing Bedtime (get_actual_bedtime)..."

# A. Direct SQLite (Baseline)
# Note: DB stores milliseconds, db.sh divides by 1000.
RAW_BEDTIME_MS=$($SQLITE "$TEMP_DB" "SELECT TIMESTAMP FROM XIAOMI_SLEEP_TIME_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 1;")
if [ -n "$RAW_BEDTIME_MS" ]; then
    # Use awk to avoid shell integer overflow with milliseconds (13 digits)
    DIRECT_BEDTIME=$(echo "$RAW_BEDTIME_MS" | awk '{print int($1/1000)}')
    READABLE_DIRECT=$(date -d @$DIRECT_BEDTIME '+%Y-%m-%d %H:%M:%S')
else
    DIRECT_BEDTIME=""
    READABLE_DIRECT="None"
fi

# B. db.sh Function
FUNC_BEDTIME=$(get_actual_bedtime)

# C. Comparison
echo "   Direct (Raw): $RAW_BEDTIME_MS ms -> $DIRECT_BEDTIME s ($READABLE_DIRECT)"
echo "   db.sh Func:   $FUNC_BEDTIME"

if [ "$DIRECT_BEDTIME" = "$FUNC_BEDTIME" ]; then
    echo "✅ Match!"
else
    echo "❌ Mismatch!"
fi

echo "------------------------------------------"

# 3. Compare Sleep Stage
echo "[3] Comparing Sleep Stage (get_current_sleep_stage)..."

# A. Direct SQLite
DIRECT_STAGE=$($SQLITE "$TEMP_DB" "SELECT STAGE FROM XIAOMI_SLEEP_STAGE_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 1;")

# B. db.sh Function
FUNC_STAGE=$(get_current_sleep_stage)

# C. Comparison
echo "   Direct:     $DIRECT_STAGE"
echo "   db.sh Func:   $FUNC_STAGE"

if [ "$DIRECT_STAGE" = "$FUNC_STAGE" ]; then
    echo "✅ Match!"
else
    echo "❌ Mismatch!"
fi

echo "------------------------------------------"

# 4. Compare User Activity
echo "[4] Comparing Activity (is_user_active)..."
echo "    Threshold: $INTENSITY_THRESHOLD, Window: 20 mins"

# A. Direct SQLite Logic
NOW_SEC=$(date +%s)
CHECK_START=$((NOW_SEC - 1200))
DIRECT_COUNT=$($SQLITE "$TEMP_DB" "SELECT COUNT(*) FROM XIAOMI_ACTIVITY_SAMPLE WHERE TIMESTAMP > $CHECK_START AND RAW_INTENSITY > $INTENSITY_THRESHOLD;")

if [ "$DIRECT_COUNT" -gt 0 ]; then
    DIRECT_RESULT="Active (Count: $DIRECT_COUNT)"
    DIRECT_BOOL=0
else
    DIRECT_RESULT="Inactive (Count: 0)"
    DIRECT_BOOL=1
fi

# B. db.sh Function
if is_user_active; then
    FUNC_RESULT="Active"
    FUNC_BOOL=0
else
    FUNC_RESULT="Inactive"
    FUNC_BOOL=1
fi

# C. Comparison
echo "   Direct:     $DIRECT_RESULT"
echo "   db.sh Func:   $FUNC_RESULT"

if [ "$DIRECT_BOOL" -eq "$FUNC_BOOL" ]; then
    echo "✅ Match!"
else
    echo "❌ Mismatch!"
fi

echo "=========================================="
# Cleanup
rm "${TEMP_DB}"*