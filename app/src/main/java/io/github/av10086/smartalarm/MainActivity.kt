package io.github.av10086.smartalarm

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.SQLiteDatabase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnReadDb = findViewById<Button>(R.id.btn_read_db)
        val tvDbContent = findViewById<TextView>(R.id.tv_db_content)

        // 初始化 libsu
        Shell.getShell()

        btnReadDb.setOnClickListener {
            val dbPath = "/data/data/nodomain.freeyourgadget.gadgetbridge/databases/gadgetbridge"
            try {
                // 使用 libsu 的 SQLiteDatabase 配合 root 权限读取
                val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery("SELECT * FROM MI_BAND_ACTIVITY_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 1", null)

                if (cursor.moveToFirst()) {
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("TIMESTAMP"))
                    val rawIntensity = cursor.getInt(cursor.getColumnIndexOrThrow("RAW_INTENSITY"))
                    tvDbContent.text = "Root 读取成功:\n时间: $timestamp\n强度: $rawIntensity"
                } else {
                    tvDbContent.text = "数据库为空"
                }
                cursor.close()
                db.close()
            } catch (e: Exception) {
                tvDbContent.text = "读取失败 (请检查 Root 权限):\n${e.message}"
            }
        }
    }
}