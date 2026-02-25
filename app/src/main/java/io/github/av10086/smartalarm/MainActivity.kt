package io.github.av10086.smartalarm

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell
import android.database.sqlite.SQLiteDatabase
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var tvRootStatus: TextView
    private lateinit var tvGbStatus: TextView
    private lateinit var tvDbContent: TextView
    private lateinit var etWakeupTime: TextInputEditText
    private lateinit var etSleepDuration: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
    try {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 初始化视图
        tvRootStatus = findViewById(R.id.tv_root_status)
        tvGbStatus = findViewById(R.id.tv_gb_status)
        tvDbContent = findViewById(R.id.tv_db_content)
        etWakeupTime = findViewById(R.id.et_wakeup_time)
        etSleepDuration = findViewById(R.id.et_sleep_duration)
        val btnSaveConfig = findViewById<Button>(R.id.btn_save_config)
        val btnReadDb = findViewById<Button>(R.id.btn_read_db)

        // 执行自检
        runSelfCheck()

        btnSaveConfig.setOnClickListener {
            saveConfig()
        }

        btnReadDb.setOnClickListener {
            testReadDatabase()
        }
    } catch (t: Throwable) {
        android.util.Log.e("SmartAlarm", "Crash in onCreate", t)
        throw t
    }
}

    private fun runSelfCheck() {
        // 1. 检测 Root 权限 (异步)
        Shell.getShell { shell ->
            if (shell.isRoot) {
                tvRootStatus.text = "Root 权限: 已获得"
                tvRootStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                
                // 2. 只有在有 Root 时才检测 Gadgetbridge
                checkGadgetbridge()
            } else {
                tvRootStatus.text = "Root 权限: 未获得 (请在管理器中授权)"
                tvRootStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                tvGbStatus.text = "Gadgetbridge: 无法检测 (需要 Root)"
            }
        }
    }

    private fun checkGadgetbridge() {
        val dbPath = "/data/data/nodomain.freeyourgadget.gadgetbridge/databases/gadgetbridge"
        // 使用 Shell 命令检查文件是否存在，避开权限问题
        val result = Shell.cmd("ls $dbPath").exec()
        if (result.isSuccess) {
            tvGbStatus.text = "Gadgetbridge: 已安装并找到数据库"
            tvGbStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvGbStatus.text = "Gadgetbridge: 未找到数据库"
            tvGbStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun saveConfig() {
        val wakeup = etWakeupTime.text.toString()
        val duration = etSleepDuration.text.toString()
        
        if (wakeup.isEmpty() || duration.isEmpty()) {
            Toast.makeText(this, "请填写完整配置", Toast.LENGTH_SHORT).show()
            return
        }

        // 这里可以将配置写入系统文件或 SharedPreferences
        // 模拟保存
        Toast.makeText(this, "配置已保存至缓存 (功能待完善)", Toast.LENGTH_SHORT).show()
    }

    private fun testReadDatabase() {
        val dbPath = "/data/data/nodomain.freeyourgadget.gadgetbridge/databases/gadgetbridge"
        
        tvDbContent.text = "正在尝试读取..."

        try {
            // 使用 libsu 的 SQLiteDatabase 配合 Root 权限
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT * FROM MI_BAND_ACTIVITY_SAMPLE ORDER BY TIMESTAMP DESC LIMIT 1", null)

            if (cursor.moveToFirst()) {
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("TIMESTAMP"))
                val rawIntensity = cursor.getInt(cursor.getColumnIndexOrThrow("RAW_INTENSITY"))
                tvDbContent.text = "读取成功!\n时间戳: $timestamp\n强度: $rawIntensity"
            } else {
                tvDbContent.text = "数据库连接成功，但表中无数据"
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            tvDbContent.text = "读取失败: ${e.message}"
        }
    }
}
