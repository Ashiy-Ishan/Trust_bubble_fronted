package com.yourdomain.bubbletrust.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.yourdomain.bubbletrust.databinding.ActivityMainBinding
import com.yourdomain.bubbletrust.services.BubbleService
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaProjectionManager: MediaProjectionManager

    // Launcher for the screen capture permission request
    private val screenCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Permission granted, now start the service with the permission data
                val serviceIntent = Intent(this, BubbleService::class.java).apply {
                    putExtra("resultCode", result.resultCode)
                    putExtra("data", result.data)
                }
                startService(serviceIntent)
                Toast.makeText(this, "Bubble started!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Screen capture permission is required to start the bubble.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        checkPermissions()

        // In MainActivity.kt -> onCreate()
        binding.btnStartBubble.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                // Just start the service. It will handle the rest.
                startService(Intent(this, BubbleService::class.java))
                Toast.makeText(this, "Bubble starting...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopBubble.setOnClickListener {
            stopService(Intent(this, BubbleService::class.java))
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        // Notification permission might also be needed for foreground services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }
}