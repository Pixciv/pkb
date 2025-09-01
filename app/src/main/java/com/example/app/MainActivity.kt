package com.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.text = "Hello from ANKD App! 👋"
        textView.textSize = 24f
        textView.setPadding(32, 64, 32, 64)

        setContentView(textView)
    }
}
