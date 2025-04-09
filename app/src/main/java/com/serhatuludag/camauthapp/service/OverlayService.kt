package com.serhatuludag.camauthapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.serhatuludag.camauthapp.R
import com.serhatuludag.camauthapp.data.model.OverlayData

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_LONG).show()
                return START_NOT_STICKY
            }
            
            val overlayData = intent.getParcelableExtra<OverlayData>(EXTRA_OVERLAY_DATA)
            if (overlayData != null) {
                Log.d(TAG, "Received overlay data: type=${overlayData.type}, content=${overlayData.content.take(50)}...")
                showOverlay(overlayData)
            } else {
                Log.e(TAG, "Overlay data is null")
            }
        } else if (intent?.action == ACTION_HIDE_OVERLAY) {
            hideOverlay()
        }
        return START_STICKY
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camera Overlay")
            .setContentText("Overlay service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun showOverlay(overlayData: OverlayData) {
        hideOverlay() // Remove any existing overlay

        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        Log.d(TAG, "Screen dimensions: width=$screenWidth, height=$screenHeight")
        Log.d(TAG, "Overlay data: type=${overlayData.type}, width=${overlayData.width}, height=${overlayData.height}, x=${overlayData.positionX}, y=${overlayData.positionY}")

        // Calculate position and dimensions based on screen dimensions
        val x = ((overlayData.positionX / 1000f) * screenWidth).toInt()
        val y = ((overlayData.positionY / 1000f) * screenHeight).toInt()
        val width = ((overlayData.width / 1000f) * screenWidth).toInt()
        val height = ((overlayData.height / 1000f) * screenHeight).toInt()

        Log.d(TAG, "Calculated dimensions: x=$x, y=$y, width=$width, height=$height")

        val layoutParams = WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        try {
            overlayView = when (overlayData.type) {
                "text" -> {
                    TextView(this).apply {
                        text = overlayData.content
                        setTextColor(resources.getColor(android.R.color.white, theme))
                        setBackgroundColor(resources.getColor(android.R.color.black, theme))
                    }
                }
                "image" -> {
                    ImageView(this).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        try {
                            val imageBytes = android.util.Base64.decode(overlayData.content, android.util.Base64.DEFAULT)
                            Log.d(TAG, "Image bytes decoded, length: ${imageBytes.size}")
                            
                            // Create a bitmap from the bytes
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (bitmap != null) {
                                setImageBitmap(bitmap)
                                Log.d(TAG, "Bitmap created and set successfully")
                            } else {
                                Log.e(TAG, "Failed to create bitmap from image bytes")
                                Toast.makeText(this@OverlayService, "Failed to load image", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading image", e)
                            Toast.makeText(this@OverlayService, "Error loading image: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                else -> {
                    Log.e(TAG, "Unknown overlay type: ${overlayData.type}")
                    null
                }
            }

            overlayView?.let { view ->
                try {
                    windowManager?.addView(view, layoutParams)
                    Log.d(TAG, "Overlay view added to window manager")
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding view to window manager", e)
                    Toast.makeText(this, "Error adding overlay: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
            Toast.makeText(this, "Failed to show overlay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                Log.d(TAG, "Overlay view removed from window manager")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding overlay", e)
            }
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OverlayService"
        const val CHANNEL_ID = "overlay_service_channel"
        const val ACTION_SHOW_OVERLAY = "com.serhatuludag.camauthapp.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.serhatuludag.camauthapp.action.HIDE_OVERLAY"
        const val EXTRA_OVERLAY_DATA = "extra_overlay_data"
    }
} 