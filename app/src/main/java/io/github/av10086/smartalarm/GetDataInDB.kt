package io.github.av10086.smartalarm

import android.database.sqlite.SQLiteDatabase

object GetDataInDB {
    private const val DB_PATH = "/data/data/nodomain.freeyourgadget.gadgetbridge/databases/gadgetbridge"

    fun queryLatestData(): String {
        return try {
            // 在 GB 进程内直接打开数据库
            val db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT * FROM MI_BAND_ACTIVITY_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 1", null)

            val result = if (cursor.moveToFirst()) {
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("TIMESTAMP"))
                val rawIntensity = cursor.getInt(cursor.getColumnIndexOrThrow("RAW_INTENSITY"))
                "最新同步时间: $timestamp, 强度: $rawIntensity"
            } else {
                "数据库中没有数据"
            }
            cursor.close()
            db.close()
            result
        } catch (e: Exception) {
            "查询失败: ${e.message}"
        }
    }
}