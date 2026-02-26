package io.github.av10086.smartalarm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import java.io.File
import java.util.Calendar
import android.app.AlarmManager
import java.text.SimpleDateFormat
import java.util.Locale

object SetAlarm {

    /**
     * 对应 alarm_test.sh + alarm.sh 的逻辑
     * 计算下一分钟的时间，寻找铃声 URI，并调用系统应用设置闹钟
     */
    fun TestAlarmFunction(context: Context) {
        SA.util.logsave("SetAlarm: 开始执行闹钟测试")

        // 1. 计算时间
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 1)
        val targetHour = calendar.get(Calendar.HOUR_OF_DAY)
        val targetMinute = calendar.get(Calendar.MINUTE)

        // 2. 决定使用哪个铃声 URI
        var ringtoneUriStr: String? = null
        val customPath = Config.RINGTONE_PATH

        if (customPath.isNotEmpty()) {
            // 逻辑 A: 使用用户指定的外部文件
            val file = File(customPath)
            if (file.exists()) {
                val uri = getMediaUriFromName(context, file.name)
                ringtoneUriStr = uri?.toString() ?: Uri.fromFile(file).toString()
                SA.util.logsave("SetAlarm: 使用自定义外部铃声: $ringtoneUriStr")
            }
        }

        // 如果上面没获取到 URI（路径为空或文件不存在），使用内置默认铃声
        if (ringtoneUriStr == null) {
            // 逻辑 B: 使用 res/raw 中的内置铃声
            // 格式: android.resource://包名/raw/文件名
            val packageName = context.packageName
            val resId = context.resources.getIdentifier(Config.DEFAULT_RINGTONE_RES, "raw", packageName)

            if (resId != 0) {
                ringtoneUriStr = "android.resource://$packageName/$resId"
                SA.util.logsave("SetAlarm: 使用内置默认铃声 URI: $ringtoneUriStr")
            } else {
                SA.util.logsave("SetAlarm: 错误 - 未找到内置铃声资源")
            }
        }

        val logDetails = """
            [闹钟属性确认]
            - 设定响铃时间: ${String.format("%02d:%02d", targetHour, targetMinute)}
            - 铃声源: ${ringtoneUriStr ?: "系统默认"}
            - 预计响铃时长: ${Config.RINGTONE_DURATION}s
            - 预计响铃间隔: ${Config.RINGTONE_INTERVAL}s
            - 预计重复次数: ${Config.RINGTONE_COUNT}
        """.trimIndent()
        SA.util.logsave(logDetails)

        // 3. 发送 Intent (保持不变)
        setSystemAlarm(context, targetHour, targetMinute, "Smart Alarm 测试", ringtoneUriStr)
        getNextAlarmTime(context)
    }

    private fun setSystemAlarm(context: Context, hour: Int, minute: Int, message: String, ringtoneUriStr: String?) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)  // 静默设置，跳过闹钟应用界面
            putExtra(AlarmClock.EXTRA_VIBRATE, true)

            // 如果成功获取了自定义铃声的 URI 就传入，否则不传（系统自动用默认铃声）
            if (!ringtoneUriStr.isNullOrEmpty()) {
                putExtra(AlarmClock.EXTRA_RINGTONE, ringtoneUriStr)
            }

            // 确保从任意上下文启动 Activity 都不会崩溃
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            SA.util.logsave("SetAlarm: 成功发送设置闹钟请求")
        } catch (e: Exception) {
            SA.util.logsave("SetAlarm: 发送闹钟请求失败: ${e.message}")
        }
    }

    /**
     * 等效于: adb shell content query --uri content://media/external/audio/media --projection _id --where "_display_name='$fileName'"
     */
    private fun getMediaUriFromName(context: Context, fileName: String): Uri? {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val id = cursor.getLong(idColumn)
                    return Uri.withAppendedPath(uri, id.toString())
                }
            }
        } catch (e: Exception) {
            SA.util.logsave("SetAlarm: 查询媒体库异常: ${e.message}")
        }
        return null
    }

    fun getNextAlarmTime(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarm = am.nextAlarmClock

        if (nextAlarm != null) {
            val triggerTime = nextAlarm.triggerTime
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(java.util.Date(triggerTime))

            val creator = nextAlarm.showIntent?.creatorPackage ?: "未知"
            SA.util.logsave("SetAlarm: [系统层面确认] 下一个闹钟将在 $dateStr 响铃 (来自包名: $creator)")
        } else {
            SA.util.logsave("SetAlarm: [系统层面确认] 警告：系统中没有发现已启用的闹钟！")
        }
    }
}