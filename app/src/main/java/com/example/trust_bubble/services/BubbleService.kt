package com.example.trust_bubble.services // Or your correct package name

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.os.Looper
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.trust_bubble.R
import com.example.trust_bubble.network.RetrofitClient
import com.example.trust_bubble.ui.PermissionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class BubbleService : Service() {

    // --- All Class Variables Declared Here ---
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var pulseAnimatorSet: AnimatorSet? = null
    private var analyzingPopupView: View? = null
    private var resultPopupView: View? = null
    private val popupHandler = Handler(Looper.getMainLooper())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private val screenDensity by lazy { Resources.getSystem().displayMetrics.densityDpi }
    private val screenWidth by lazy { Resources.getSystem().displayMetrics.widthPixels }
    private val screenHeight by lazy { Resources.getSystem().displayMetrics.heightPixels }
    private var hasPermission = false
    private var isFirstClick = true
    private var lastClickTime: Long = 0
    private val FIRST_CLICK_COOLDOWN_MS: Long = 2000
    private val SUBSEQUENT_CLICK_COOLDOWN_MS: Long = 4000

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "TrustBubbleChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Initialize all necessary components
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val handlerThread = HandlerThread("ScreenCapture")
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)

        setupBubbleView()
        setupPulseAnimator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("data")
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            val mediaProjectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    hasPermission = false
                    stopSelf() // Stop the service if permission is revoked
                }
            }
            mediaProjection?.registerCallback(mediaProjectionCallback, backgroundHandler)

            hasPermission = true
            setupScreenCapture()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
        bubbleParams = WindowManager.LayoutParams(
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
                        initialX = bubbleParams.x; initialY = bubbleParams.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        isClick = true; return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX; val dy = event.rawY - initialTouchY
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) isClick = false
                        bubbleParams.x = initialX + dx.toInt(); bubbleParams.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(bubbleView, bubbleParams); return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            val currentCooldown = if (isFirstClick) FIRST_CLICK_COOLDOWN_MS else SUBSEQUENT_CLICK_COOLDOWN_MS
                            if (System.currentTimeMillis() - lastClickTime < currentCooldown) {
                                Toast.makeText(this@BubbleService, "Please wait...", Toast.LENGTH_SHORT).show()
                            } else {
                                lastClickTime = System.currentTimeMillis()
                                if (hasPermission) {
                                    captureScreenAndAnalyze()
                                } else {
                                    val intent = Intent(this@BubbleService, PermissionActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                                if (isFirstClick) {
                                    isFirstClick = false
                                }
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })
        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun sendToServer(imageBytes: ByteArray) {
        // Start the "Analyzing" visual state
        popupHandler.post {
            showAnalyzingPopup()
            pulseAnimatorSet?.start()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val response = RetrofitClient.instance.analyzeScreenshot(requestFile)

                withContext(Dispatchers.Main) {
                    // Stop the "Analyzing" state
                    hideAnalyzingPopup()
                    pulseAnimatorSet?.cancel()
                    bubbleView.scaleX = 1f; bubbleView.scaleY = 1f

                    // Show the final result
                    if (response.isSuccessful) {
                        val classification = response.body()?.classification ?: "Unknown"
                        showResultPopup(classification)
                    } else {
                        Toast.makeText(this@BubbleService, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }

                    // Start the "Cooldown" visual state
                    val currentCooldown = if (isFirstClick) FIRST_CLICK_COOLDOWN_MS else SUBSEQUENT_CLICK_COOLDOWN_MS
                    bubbleView.alpha = 0.5f
                    popupHandler.postDelayed({ bubbleView.alpha = 1.0f }, currentCooldown)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Always stop the "Analyzing" state on failure
                    hideAnalyzingPopup()
                    pulseAnimatorSet?.cancel()
                    bubbleView.scaleX = 1f; bubbleView.scaleY = 1f
                    Toast.makeText(this@BubbleService, "Failure: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- All Helper Functions Below ---

    private fun setupPulseAnimator() {
        val scaleX = ObjectAnimator.ofFloat(bubbleView, "scaleX", 1f, 1.2f).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val scaleY = ObjectAnimator.ofFloat(bubbleView, "scaleY", 1f, 1.2f).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        pulseAnimatorSet = AnimatorSet().apply {
            play(scaleX).with(scaleY)
        }
    }

    private fun showResultPopup(classification: String) {
        popupHandler.removeCallbacksAndMessages(null)
        resultPopupView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_TrustBubble)
        val inflater = LayoutInflater.from(themedContext)
        resultPopupView = inflater.inflate(R.layout.popup_result_layout, null)

        val icon = resultPopupView?.findViewById<ImageView>(R.id.iv_status_icon)
        val text = resultPopupView?.findViewById<TextView>(R.id.tv_status_text)

        if (classification.equals("Good", ignoreCase = true)) {
            icon?.setImageResource(R.drawable.ic_success)
            text?.text = "Result: Good"
        } else {
            icon?.setImageResource(R.drawable.ic_warning)
            text?.text = "Result: Harmful"
        }

        val popupParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleParams.x + bubbleView.width + 16
            y = bubbleParams.y + (bubbleView.height / 4)
        }
        windowManager.addView(resultPopupView, popupParams)

        popupHandler.postDelayed({
            resultPopupView?.let { viewToRemove ->
                try {
                    windowManager.removeView(viewToRemove)
                    resultPopupView = null
                } catch (e: Exception) {}
            }
        }, 4000)
    }

    private fun showAnalyzingPopup() {
        hideAnalyzingPopup()
        val themedContext = ContextThemeWrapper(this, R.style.Theme_TrustBubble)
        analyzingPopupView = LayoutInflater.from(themedContext).inflate(R.layout.popup_analyzing_layout, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleParams.x + bubbleView.width + 16
            y = bubbleParams.y + (bubbleView.height / 2) - 40
        }
        windowManager.addView(analyzingPopupView, params)
    }

    private fun hideAnalyzingPopup() {
        analyzingPopupView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            analyzingPopupView = null
        }
    }

    private fun setupScreenCapture() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture", screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, backgroundHandler
        )
    }

    private fun captureScreenAndAnalyze() {
        if (!hasPermission || imageReader == null) return
        val image = imageReader?.acquireLatestImage() ?: return

        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth
        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        val smallerImageBytes = resizeAndCompressImage(bitmap)
        sendToServer(smallerImageBytes)
    }

    private fun resizeAndCompressImage(bitmap: Bitmap, maxWidth: Int = 1080, quality: Int = 80): ByteArray {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val ratio = originalWidth.toFloat() / originalHeight.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (originalWidth > maxWidth) {
            targetWidth = maxWidth
            targetHeight = (targetWidth / ratio).toInt()
        } else {
            targetWidth = originalWidth
            targetHeight = originalHeight
        }
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "TrustBubble Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("TrustBubble is Active")
            .setContentText("Tap the bubble to analyze screen content.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimatorSet?.cancel()
        hideAnalyzingPopup()
        resultPopupView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        virtualDisplay?.release()
        mediaProjection?.stop()
        if (::bubbleView.isInitialized) {
            windowManager.removeView(bubbleView)
        }
    }
}