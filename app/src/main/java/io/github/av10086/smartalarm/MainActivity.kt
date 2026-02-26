package io.github.av10086.smartalarm

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    // 在较新的 Android 系统中，为了防止应用互相窃取数据，系统采用了极强的沙盒机制（App Data Isolation）。
    // 当通过 Shell.getShell {} 获取 Root 权限时，默认情况下，这个 Root Shell 继承的是 SmartAlarm 这个 App 的挂载环境。在这个被隔离的环境里，其他 App（如 Gadgetbridge）的 /data/data/ 目录被系统直接“隐藏”或“卸载”了。因此，尽管是 Root，但在这个平行世界里，那个文件确实“不存在”，导致 [ -f ] 和 cp 命令静默失败（Code 1）。
    // init 块，配置 libsu 使用全局挂载空间
    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )
    }

    private lateinit var tvRootStatus: TextView
    private lateinit var tvGbStatus: TextView
    private lateinit var tvDbContent: TextView
    private lateinit var etWakeupTime: TextInputEditText
    private lateinit var etSleepDuration: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRootStatus = findViewById(R.id.tv_root_status)
        tvGbStatus = findViewById(R.id.tv_gb_status)
        tvDbContent = findViewById(R.id.tv_db_content)
        etWakeupTime = findViewById(R.id.et_wakeup_time)
        etSleepDuration = findViewById(R.id.et_sleep_duration)
        val btnSaveConfig = findViewById<Button>(R.id.btn_save_config)
        val btnReadDb = findViewById<Button>(R.id.btn_read_db)
        val btnTestAlarm = findViewById<Button>(R.id.btn_test_alarm)

        SA.util.logsave("MainActivity: App 启动")

        // 异步获取 Shell 并执行自检
        Shell.getShell { shell ->
            if (shell.isRoot) {
                runOnUiThread {
                    tvRootStatus.text = "Root 权限: 已获得"
                    tvRootStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    SA.util.logsave("MainActivity: Root 已授权")
                    checkGadgetbridge()
                }
            } else {
                runOnUiThread {
                    tvRootStatus.text = "Root 权限: 未获得"
                    tvRootStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    SA.util.logsave("MainActivity: Root 未授权")
                }
            }
        }

        btnSaveConfig.setOnClickListener {
            SA.util.logsave("MainActivity: 保存配置: ${etWakeupTime.text}")
            Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
        }

        btnTestAlarm.setOnClickListener {
            Toast.makeText(this, "正在设置闹钟...", Toast.LENGTH_SHORT).show()
            SetAlarm.TestAlarmFunction(this)
        }

        btnReadDb.setOnClickListener {
            SA.util.logsave("MainActivity: 手动触发数据库读取测试")
            tvDbContent.text = "正在尝试读取..."

            // 使用新线程或协程避免阻塞主线程
            Thread {
                val result = GetDataInDB.queryLatestData(this)
                runOnUiThread {
                    tvDbContent.text = result
                }
            }.start()
        }
    }

    private fun checkGadgetbridge() {
        // 改用 [ -f ] 这种更原生的 Shell 方式检测文件是否存在
        val dbPath = Config.DB_PATH
        SA.util.logsave("MainActivity: 正在检测数据库: $dbPath")
        
        Shell.cmd("[ -f \"$dbPath\" ]").submit { result ->
            runOnUiThread {
                if (result.isSuccess) {
                    tvGbStatus.text = "Gadgetbridge: 已找到数据库"
                    tvGbStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    SA.util.logsave("MainActivity: 数据库检测通过")
                } else {
                    tvGbStatus.text = "Gadgetbridge: 未找到数据库"
                    tvGbStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    SA.util.logsave("MainActivity: 数据库文件不存在或权限不足")
                }
            }
        }
    }
    private fun testReadDatabase() {
        SA.util.logsave("MainActivity: 手动触发数据库读取测试")
        tvDbContent.text = "正在尝试读取..."
        
        // 传入上下文以便获取缓存目录
        val result = GetDataInDB.queryLatestData(this)
        tvDbContent.text = result
    }
}
