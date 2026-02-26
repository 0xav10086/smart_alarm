package io.github.av10086.smartalarm

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 增强型日志保存工具，支持日志滚动和数量限制
 */
object SA {
    object util {
        private const val TAG = "SmartAlarm_Log"
        private var logFile: File? = null

        /**
         * 保存日志
         */
        fun logsave(message: String) {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logLine = "[$timeStamp] $message\n"

            Log.d(TAG, message)

            try {
                if (logFile == null) {
                    initLogFile()
                }
                logFile?.let {
                    FileOutputStream(it, true).use { stream ->
                        stream.write(logLine.toByteArray())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save log: ${e.message}")
            }
        }

        /**
         * 初始化日志系统
         */
        private fun initLogFile() {
            val logDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), Config.LOG_DIR_NAME)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val latestLog = File(logDir, "latest.log")
            
            // 1. 滚动旧日志：如果 latest.log 已存在，将其重命名为时间戳备份
            if (latestLog.exists()) {
                val lastModified = Date(latestLog.lastModified())
                // 使用 : 在某些文件系统上会有问题，建议改用 -
                val backupName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(lastModified) + ".log"
                val backupFile = File(logDir, backupName)
                latestLog.renameTo(backupFile)
            }

            // 2. 锁定新的 latest.log
            logFile = latestLog
            
            // 3. 记录初始化信息 (直接写入，避免递归调用 logsave)
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val initMsg = "[$timeStamp] [INIT] 日志系统已启动。输出路径: ${latestLog.absolutePath}\n"
            try {
                FileOutputStream(latestLog, true).use { stream ->
                    stream.write(initMsg.toByteArray())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init write failed: ${e.message}")
            }
            
            // 4. 限制文件数量
            limitLogFileNum(logDir)
        }

        /**
         * 限制日志文件数量，删除最早的文件
         */
        private fun limitLogFileNum(logDir: File) {
            val files = logDir.listFiles { _, name -> name.endsWith(".log") && name != "latest.log" }
            if (files != null && files.size > Config.MAX_LOG_FILES) {
                val sortedFiles = files.sortedBy { it.lastModified() }
                val deleteCount = sortedFiles.size - Config.MAX_LOG_FILES
                for (i in 0 until deleteCount) {
                    sortedFiles[i].delete()
                }
            }
        }
    }
}
