package io.github.av10086.smartalarm

/**
 * 全局配置中心
 */
object Config {
    // Gadgetbridge 数据库路径
    const val DB_PATH = "/data/data/nodomain.freeyourgadget.gadgetbridge/databases/Gadgetbridge"
    
    // 日志配置
    const val LOG_DIR_PARENT = "Documents" // 放在 Documents 目录下
    const val LOG_DIR_NAME = "SmartAlarm/logs"
    const val MAX_LOG_FILES = 10
}
