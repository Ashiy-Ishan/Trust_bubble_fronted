package com.yourdomain.bubbletrust.services

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.*
import android.widget.Toast
import com.yourdomain.bubbletrust.R
import com.yourdomain.bubbletrust.network.RetrofitClient
import com.yourdomain.bubbletrust.ui.PermissionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class BubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null
    private val screenDensity by lazy { Resources.getSystem().displayMetrics.densityDpi }
    private val screenWidth by lazy { Resources.getSystem().displayMetrics.widthPixels }
    private val screenHeight by lazy { Resources.getSystem().displayMetrics.heightPixels }
    private var hasPermission = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "BubbleTrustChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        setupBubbleView()

        val handlerThread = HandlerThread("ScreenCapture")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The new, safe method for Android 13+
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            // The old method, with a note to the compiler that we know it's old
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("data")
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            // Get the permission token
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            // --- THIS IS THE NEW CODE YOU NEED TO ADD ---
            // Create the "fire drill plan" (the callback)
            val mediaProjectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    // This is what happens if the capture is stopped unexpectedly.
                    // We clean up and stop the service.
                    hasPermission = false
                    stopSelf()
                }
            }
            // Register the callback with the system
            mediaProjection?.registerCallback(mediaProjectionCallback, handler)
            // --- END OF NEW CODE ---

            hasPermission = true
            setupScreenCapture()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        // This function's content remains the same, except for the onTouch listener's ACTION_UP
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0; private var initialY = 0
            private var initialTouchX = 0f; private var initialTouchY = 0f
            private var isClick = true
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        isClick = true; return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX; val dy = event.rawY - initialTouchY
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isClick = false
                        params.x = initialX + dx.toInt(); params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(bubbleView, params); return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            if (hasPermission) {
                                captureScreenAndAnalyze()
                            } else {
                                // No permission yet, so launch our invisible activity to get it
                                val intent = Intent(this@BubbleService, PermissionActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })
        windowManager.addView(bubbleView, params)
    }

    private fun setupScreenCapture() {
        // (This function's content is unchanged)
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture", screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, handler
        )
    }

    private fun captureScreenAndAnalyze() {
        // (This function's content is unchanged)
        if (mediaProjection == null || imageReader == null) return
        Toast.makeText(this, "Capturing...", Toast.LENGTH_SHORT).show()
        val image = imageReader?.acquireLatestImage() ?: return
        val planes = image.planes; val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride; val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth
        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer); image.close()
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        sendToServer(outputStream.toByteArray())
    }

    private fun sendToServer(imageBytes: ByteArray) {
        // (This function's content is unchanged)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestFile = imageBytes.toRequestBody("image/png".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", "screenshot.png", requestFile)
                val response = RetrofitClient.instance.analyzeScreenshot(body)
                CoroutineScope(Dispatchers.Main).launch {
                    val toastMessage = if (response.isSuccessful) {
                        "Analysis: ${response.body()?.classification}"
                    } else {
                        "Error: ${response.message()}"
                    }
                    Toast.makeText(this@BubbleService, toastMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@BubbleService, "Failure: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Notification Functions Required for Foreground Service ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "BubbleTrust Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("BubbleTrust is Active")
                .setContentText("Tap the bubble to analyze screen content.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        } else {
            // Add this suppression note for the old method
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("BubbleTrust is Active")
                .setContentText("Tap the bubble to analyze screen content.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
        if (::bubbleView.isInitialized) {
            windowManager.removeView(bubbleView)
        }
    }
}