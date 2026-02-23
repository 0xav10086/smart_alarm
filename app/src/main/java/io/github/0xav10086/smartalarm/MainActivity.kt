package io.github.av10086.smartalarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建一个简单的界面告诉用户模块状态
        val textView = TextView(this).apply {
            text = "Smart Alarm Xposed Helper\n\n1. 请在 LSPosed 中激活本模块\n2. 勾选 Gadgetbridge 作为作用域\n3. 重启 Gadgetbridge"
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        setContentView(textView)
    }
}