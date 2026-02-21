#!/system/bin/sh

send_notification() {
    cmd notification post -S bigtext -t "Smart Alarm" "Tag" "$1" >/dev/null 2>&1
}

trigger_alarm() {
    log_info "!!! ALARM TRIGGERED: $1 !!!"
    
    # Wake up screen
    input keyevent KEYCODE_WAKEUP
    
    # [Auto-Select Ringtone] Handle empty or invalid path
    local current_ringtone="$RINGTONE_PATH"
    
    # Debug log for initial path
    log_info "Initial ringtone path: '$current_ringtone'"

    if [ -z "$current_ringtone" ] || [ ! -f "$current_ringtone" ]; then
        log_info "Ringtone path is empty or invalid. Searching for fallback..."
        
        # Use MODDIR if defined (from service.sh or test script), otherwise current dir
        local base_dir="${MODDIR:-.}"
        # Ensure absolute path (Fix for music app failing to open relative paths)
        case "$base_dir" in
            /*) ;;
            *) base_dir="$(cd "$base_dir" && pwd)" ;;
        esac
        
        # Search paths: System paths + Module webroot (using absolute path via MODDIR)
        for d in "/system/media/audio/alarms" "/system_ext/media/audio/alarms" "/product/media/audio/alarms" "/system/product/media/audio/alarms" "$base_dir/webroot"; do
          # 我在抱什么幻想，$base_dir/webroot可读吗？
            log_info "Checking directory: $d"
            if [ -d "$d" ]; then
                # Find first file (mp3/ogg/wav/m4a) or just first file
                local f=$(ls "$d" | grep -E '\.(mp3|ogg|wav|m4a)$' | head -n 1)
                if [ -n "$f" ]; then
                    current_ringtone="$d/$f"
                    log_info "Auto-selected ringtone: $current_ringtone"
                    break
                else
                    log_info "  No audio files found in $d"
                fi
            else
                log_info "  Directory not found: $d"
            fi
        done
    else
        log_info "Using configured ringtone: $current_ringtone"
    fi

    local final_ringtone_path=""

    # [Prepare Public Ringtone] Copy to /sdcard/Alarms so System Alarm can read it
    if [ -n "$current_ringtone" ] && [ -f "$current_ringtone" ]; then
        # Only copy if it's not already in a public path
        case "$current_ringtone" in
            /sdcard/*|/system/*|/product/*|/vendor/*) 
                final_ringtone_path="$current_ringtone"
                ;;
            *)
                local public_dir="/sdcard/Alarms"
                if [ ! -d "$public_dir" ]; then mkdir -p "$public_dir"; fi
                
                local fname=$(basename "$current_ringtone")
                local target="$public_dir/$fname"
                
                if [ ! -f "$target" ]; then
                    log_info "Copying ringtone to public directory: $target"
                    cp "$current_ringtone" "$target"
                    # Scan file so system sees it immediately
                    am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$target" >/dev/null 2>&1
                else
                    log_info "Ringtone already exists in public directory: $target"
                fi
                final_ringtone_path="$target"
                ;;
        esac
    fi

    # [Set System Alarm] Schedule for NOW + 1 Minute
    local cur_h=$(date +%H)
    local cur_m=$(date +%M)
    
    # Calculate next minute safely
    set -- $(awk -v h=$cur_h -v m=$cur_m 'BEGIN {
        m++; 
        if(m>=60){m=0; h++}; 
        if(h>=24){h=0}; 
        printf "%02d %02d", h, m
    }')
    local next_h=$1
    local next_m=$2
    
    log_info "Scheduling System Alarm for $next_h:$next_m (Smart Alarm Triggered)"
    
    local ringtone_extra=""
    if [ -n "$final_ringtone_path" ]; then
        # Resolve Content URI for better compatibility (Fix for default ringtone issue)
        local fname=$(basename "$final_ringtone_path")
        local content_uri=""
        
        # 1. Try External Media (User files / sdcard)
        local query_out=$(content query --uri content://media/external/audio/media --projection _id --where "_display_name='$fname'")
        local id=$(echo "$query_out" | grep -o '_id=[0-9]*' | head -n 1 | cut -d'=' -f2)
        
        # Retry logic: If not found and file is in /sdcard (might be fresh copy), rescan and retry
        if [ -z "$id" ]; then
            case "$final_ringtone_path" in
                /sdcard/*)
                     log_info "Content URI not found immediately, forcing rescan..."
                     am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$final_ringtone_path" >/dev/null 2>&1
                     sleep 2
                     query_out=$(content query --uri content://media/external/audio/media --projection _id --where "_display_name='$fname'")
                     id=$(echo "$query_out" | grep -o '_id=[0-9]*' | head -n 1 | cut -d'=' -f2)
                     ;;
            esac
        fi

        if [ -n "$id" ]; then
            content_uri="content://media/external/audio/media/$id"
        else
            # 2. Try Internal Media (System files)
            query_out=$(content query --uri content://media/internal/audio/media --projection _id --where "_display_name='$fname'")
            id=$(echo "$query_out" | grep -o '_id=[0-9]*' | head -n 1 | cut -d'=' -f2)
            if [ -n "$id" ]; then
                content_uri="content://media/internal/audio/media/$id"
            fi
        fi

        if [ -n "$content_uri" ]; then
            ringtone_extra="--es android.intent.extra.alarm.RINGTONE $content_uri"
            log_info "Setting alarm ringtone to: $content_uri"
        else
            ringtone_extra="--es android.intent.extra.alarm.RINGTONE file://$final_ringtone_path"
            log_info "Setting alarm ringtone to: file://$final_ringtone_path (Fallback - Content URI resolution failed)"
        fi
    fi
    
    # Intent Extras:
    # android.intent.extra.alarm.HOUR (int)
    # android.intent.extra.alarm.MINUTES (int)
    # android.intent.extra.alarm.MESSAGE (string)
    # android.intent.extra.alarm.SKIP_UI (boolean) - Try to set without confirmation
    
    am start -a android.intent.action.SET_ALARM \
        --ei android.intent.extra.alarm.HOUR $next_h \
        --ei android.intent.extra.alarm.MINUTES $next_m \
        --es android.intent.extra.alarm.MESSAGE "Smart Alarm Wakeup" \
        $ringtone_extra \
        --ez android.intent.extra.alarm.VIBRATE true \
        --ez android.intent.extra.alarm.SKIP_UI true >/dev/null 2>&1
        
    if [ $? -eq 0 ]; then
        log_info "System Alarm set command executed successfully."
    else
        log_info "Failed to set System Alarm."
    fi
    
    ALARM_TRIGGERED_DATE=$(date +%Y-%m-%d)
    
    # Rotate log after wakeup session is complete
    if command -v rotate_log >/dev/null 2>&1; then
        rotate_log
    fi
}