package io.github.av10086.smartalarm

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.topjohnwu.superuser.Shell
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GetDataInDB {

    /**
     * 执行数据库查询并返回结果字符串
     * 仿照 legacy_ksu/db.sh 的逻辑，提取心率、睡眠时间和阶段
     */
    fun queryLatestData(context: Context): String {
        SA.util.logsave("GetDataInDB: 开始备份数据库...")
        
        val tempDbFile = File(context.cacheDir, "gb_backup.db")
        val tempDbPath = tempDbFile.absolutePath

        // 获取当前 App 的 UID，用于移交文件所有权
        val appUid = context.applicationInfo.uid

        SA.util.logsave("GetDataInDB: 开始备份数据库... App UID: $appUid")

        // 使用 Mount Master 复制文件，并修改所有者和权限
        val copyResult = Shell.cmd(
            "cp \"${Config.DB_PATH}\" \"$tempDbPath\"",
            "chown $appUid:$appUid \"$tempDbPath\"",
            "chmod 600 \"$tempDbPath\"" // 既然是自己的文件了，600 就足够安全
        ).exec()

        if (!copyResult.isSuccess) {
            val errorOut = copyResult.out.joinToString("\n")
            val errorErr = copyResult.err.joinToString("\n")
            val errorMsg = "备份失败. Code: ${copyResult.code}, Out: $errorOut, Err: $errorErr"
            SA.util.logsave("GetDataInDB: $errorMsg")
            return errorMsg
        }

        val sb = StringBuilder() // 确实是这个缩写没错，但是感觉AI在骂我:(
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return try {
            // 2. 使用标准 API 读取
            val db = SQLiteDatabase.openDatabase(tempDbPath, null, SQLiteDatabase.OPEN_READONLY)

            SA.util.logsave("GetDataInDB: 备份数据库连接成功")

            // [1] 最新心率采样 (TIMESTAMP 是秒)
            val cursorHr = db.rawQuery("SELECT TIMESTAMP, HEART_RATE FROM XIAOMI_ACTIVITY_SAMPLE WHERE HEART_RATE > 0 ORDER BY TIMESTAMP DESC LIMIT 5", null)
            if (cursorHr.moveToFirst()) {
                val ts = cursorHr.getLong(0) * 1000
                val hr = cursorHr.getInt(1)
                sb.append("💓 心率: $hr BPM (${sdf.format(Date(ts))})\n")
            } else {
                sb.append("💓 心率: 未找到数据\n")
            }
            cursorHr.close()

            // [2] 睡眠结论提取 (TIMESTAMP 是毫秒)
            val cursorSleep = db.rawQuery("SELECT TIMESTAMP, WAKEUP_TIME FROM XIAOMI_SLEEP_TIME_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 5", null)
            if (cursorSleep.moveToFirst()) {
                val startTs = cursorSleep.getLong(0)
                val endTs = cursorSleep.getLong(1)
                sb.append("🛌 睡眠: ${sdf.format(Date(startTs))} -> ${sdf.format(Date(endTs))}\n")
            } else {
                sb.append("🛌 睡眠: 未找到记录\n")
            }
            cursorSleep.close()

            // [3] 睡眠阶段分析 (TIMESTAMP 是毫秒)
            val cursorStage = db.rawQuery("SELECT TIMESTAMP, STAGE FROM XIAOMI_SLEEP_STAGE_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 5", null)
            if (cursorStage.moveToFirst()) {
                val ts = cursorStage.getLong(0)
                val stage = cursorStage.getInt(1)
                val stageStr = when(stage) {
                    1 -> "深睡"
                    2 -> "浅睡"
                    3 -> "REM"
                    else -> "清醒($stage)"
                }
                sb.append("📊 阶段: $stageStr (${sdf.format(Date(ts))})")
            } else {
                sb.append("📊 阶段: 未找到数据")
            }
            cursorStage.close()
            db.close()

            val result = sb.toString()

            SA.util.logsave("GetDataInDB: 查询结果 -> $result")
            result
        } catch (e: Exception) {
            val errorMsg = "读取备份失败: ${e.message}"
            SA.util.logsave("GetDataInDB: $errorMsg")
            errorMsg
        } finally {
            // 3. 清理
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }
            SA.util.logsave("GetDataInDB: 临时备份已清理")
        }
    }
}
