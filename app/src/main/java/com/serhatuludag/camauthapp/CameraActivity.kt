package com.serhatuludag.camauthapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.serhatuludag.camauthapp.api.AppWebSocketClient
import com.serhatuludag.camauthapp.data.model.OverlayMessage
import com.serhatuludag.camauthapp.databinding.ActivityCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var app: AccessCodeApplication
    private lateinit var cameraExecutor: ExecutorService
    private var webSocketClient: AppWebSocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as AccessCodeApplication
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check permissions and start camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Connect to WebSocket
        connectWebSocket()

        // Setup logout button
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun connectWebSocket() {
        val sessionId = intent.getStringExtra("sessionId") ?: return
        val token = intent.getStringExtra("token") ?: return
        val wsUrl = "ws://10.0.2.2:8000/ws/$sessionId"
        
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()
            
        webSocketClient = AppWebSocketClient(request, object : AppWebSocketClient.OverlayWebSocketListener {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                runOnUiThread {
                    binding.connectionStatus.text = "Connected"
                    binding.connectionStatus.setTextColor(ContextCompat.getColor(this@CameraActivity, android.R.color.holo_green_dark))
                }
            }

            override fun onOverlayMessage(webSocket: WebSocket, overlay: OverlayMessage) {
                onOverlayReceived(overlay)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    binding.connectionStatus.text = "Disconnecting: $reason"
                    binding.connectionStatus.setTextColor(ContextCompat.getColor(this@CameraActivity, android.R.color.holo_orange_dark))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    binding.connectionStatus.text = "Disconnected: $reason"
                    binding.connectionStatus.setTextColor(ContextCompat.getColor(this@CameraActivity, android.R.color.holo_red_dark))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                runOnUiThread {
                    var errorMessage = "WebSocket error: ${t.message}"
                    if (response != null) {
                        errorMessage += " (${response.code} - ${response.message})"
                    }
                    binding.connectionStatus.text = errorMessage
                    binding.connectionStatus.setTextColor(ContextCompat.getColor(this@CameraActivity, android.R.color.holo_red_dark))
                }
            }
        })
        webSocketClient?.connect()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                showError("Failed to start camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun onOverlayReceived(overlay: OverlayMessage) {
        Log.d(TAG, "Overlay received: ${overlay.data.type}")
        Log.d(TAG, "Raw overlay data: ${overlay.data}")

        runOnUiThread {
            // Clear previous overlays
            binding.overlayContainer.removeAllViews()

            // Get screen dimensions
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Calculate position and dimensions based on screen dimensions
            // Scale from 0-1000 range to actual screen dimensions
            val x = ((overlay.data.positionX / 1000f) * screenWidth).toInt()
            val y = ((overlay.data.positionY / 1000f) * screenHeight).toInt()
            val width = ((overlay.data.width / 1000f) * screenWidth).toInt()
            val height = ((overlay.data.height / 1000f) * screenHeight).toInt()

            Log.d(TAG, "Screen dimensions - Width: $screenWidth, Height: $screenHeight")
            Log.d(TAG, "Raw values - X: ${overlay.data.positionX}, Y: ${overlay.data.positionY}, Width: ${overlay.data.width}, Height: ${overlay.data.height}")
            Log.d(TAG, "Calculated position - X: $x, Y: $y, Width: $width, Height: $height")

            // Calculate margins to position the overlay
            // For X: 400 means 40% from left
            // For Y: 400 means 40% from top
            val leftMargin = x
            val topMargin = y

            Log.d(TAG, "Final margins - Left: $leftMargin, Top: $topMargin")

            val params = FrameLayout.LayoutParams(
                width,
                height
            ).apply {
                // Set position using absolute coordinates
                gravity = Gravity.NO_GRAVITY
                setMargins(leftMargin, topMargin, 0, 0)
            }

            when(overlay.data.type) {
                "text" -> {
                    val textView = TextView(this).apply {
                        text = overlay.data.content
                        layoutParams = params
                        setTextColor(ContextCompat.getColor(this@CameraActivity, android.R.color.white))
                        setBackgroundColor(ContextCompat.getColor(this@CameraActivity, android.R.color.black))
                    }
                    binding.overlayContainer.addView(textView)
                }
                "image" -> {
                    try {
                        val imageView = androidx.appcompat.widget.AppCompatImageView(this).apply {
                            layoutParams = params
                            scaleType = android.widget.ImageView.ScaleType.FIT_XY
                        }

                        val imageBytes = Base64.decode(overlay.data.content, Base64.DEFAULT)
                        Glide.with(this)
                            .load(imageBytes)
                            .into(imageView)

                        binding.overlayContainer.addView(imageView)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error displaying image overlay", e)
                        showError("Failed to display image overlay")
                    }
                }
                else -> {
                    Log.e(TAG, "Unknown overlay type: ${overlay.data.type}")
                }
            }
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // Disconnect WebSocket
                webSocketClient?.disconnect()

                // Clear session data
                app.sessionManager.clearSession()

                // Return to login activity
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        webSocketClient?.disconnect()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}