package io.github.av10086.smartalarm

/**
 * 全局配置中心
 */
object Config {
    // 期望的睡觉时间（24小时制）
    const val EXPECTED_BEDTIME = "23:00"

    // 期望的起床时间（24小时制）
    const val EXPECTED_WAKEUP = "07:00"

    // 期望的睡眠时长（小时）
    const val EXPECTED_SLEEP_DURATION = "8"

    // 闹钟铃声文件路径（空表示使用默认铃声）
    const val RINGTONE_PATH = ""

    // 默认铃声资源名称 (对应 res/raw/sa_default_ring.mp3)
    const val DEFAULT_RINGTONE_RES = "sa_default_ring"

    // 单次铃声播放时长（秒）
    const val RINGTONE_DURATION = "60"

    // 铃声播放间隔（秒）
    const val RINGTONE_INTERVAL = "300"

    // 铃声播放次数
    const val RINGTONE_COUNT = "3"

    // 是否启用温和唤醒模式
    const val GENTLE_WAKE = "true"

    // 温和唤醒窗口期（分钟）
    const val GENTLE_WAKE_WINDOW = "20"

    // 心率阈值（bpm）
    const val HR_THRESHOLD = "60"

    // 活动强度阈值
    const val INTENSITY_THRESHOLD = "60"

    // 检测窗口（分钟）
    const val DETECTION_WINDOW = "15"

    // 日志保留天数，此参数暂未使用
    // const val LOG_RETENTION = "3"

    // 轮询模式：dynamic 或 fixed
    const val POLLING_MODE = "dynamic"

    // 检查间隔（秒）
    const val CHECK_INTERVAL = "10"

    // Gadgetbridge 数据库路径
    const val DB_PATH = "/data/data/nodomain.freeyourgadget.gadgetbridge/databases/Gadgetbridge"

    // 日志配置
    const val LOG_DIR_PARENT = "Documents" // 放在 Documents 目录下
    const val LOG_DIR_NAME = "SmartAlarm/logs"
    const val MAX_LOG_FILES = 10
}
