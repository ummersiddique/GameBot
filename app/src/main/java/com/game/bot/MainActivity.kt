package com.game.bot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnEnableService).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val progressBar = findViewById<ProgressBar>(R.id.testProgressBar)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnStartTest = findViewById<Button>(R.id.btnStartTest)
        val closeAdContainer = findViewById<LinearLayout>(R.id.btnCloseAdContainer)

        btnStartTest.setOnClickListener {
            btnStartTest.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            btnNext.visibility = View.GONE
            closeAdContainer.visibility = View.GONE
            progressBar.progress = 0
            
            // Simulate progress
            val handler = Handler(Looper.getMainLooper())
            Thread {
                for (i in 1..100) {
                    Thread.sleep(30) // Simulate work
                    handler.post {
                        progressBar.progress = i
                        if (i == 100) {
                            progressBar.visibility = View.GONE
                            // Randomly show either "Next" button or "Close Ad" container
                            if (Math.random() > 0.5) {
                                btnNext.visibility = View.VISIBLE
                            } else {
                                closeAdContainer.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }.start()
        }

        btnNext.setOnClickListener {
            Toast.makeText(this, "Bot clicked Next!", Toast.LENGTH_SHORT).show()
            btnNext.visibility = View.GONE
            btnStartTest.visibility = View.VISIBLE
        }

        closeAdContainer.setOnClickListener {
            Toast.makeText(this, "Bot clicked Close Ad!", Toast.LENGTH_SHORT).show()
            closeAdContainer.visibility = View.GONE
            btnStartTest.visibility = View.VISIBLE
        }
    }
}
