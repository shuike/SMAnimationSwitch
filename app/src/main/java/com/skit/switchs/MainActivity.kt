package com.skit.switchs

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val switchView = findViewById<SwitchView>(R.id.switch_view)
        switchView.changeCallback = {
            ValueAnimator.ofArgb(
                if (it) {
                    Color.WHITE
                } else {
                    Color.BLACK
                },
                if (it) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
            ).apply {
                duration = 300
                addUpdateListener {
                    window.decorView.background = ColorDrawable(it.animatedValue as Int)
                }
                start()
            }
        }
    }
}