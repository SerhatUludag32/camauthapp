package com.serhatuludag.camauthapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.serhatuludag.camauthapp.data.model.AccessCodeValidationRequest
import com.serhatuludag.camauthapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var app: AccessCodeApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as AccessCodeApplication

        // Check if already logged in
        if (app.sessionManager.isLoggedIn()) {
            navigateToCameraActivity()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        binding.btnSubmit.setOnClickListener {
            val accessCode = binding.etAccessCode.text.toString().trim()

            if (accessCode.isEmpty()) {
                binding.tilAccessCode.error = "Please enter an access code"
                return@setOnClickListener
            }

            validateAccessCode(accessCode)
        }
    }

    private fun validateAccessCode(code: String) {
        // Show loading
        binding.progressBar.visibility= View.VISIBLE
        binding.btnSubmit.isEnabled = false

        // Collect device info
        val deviceInfo = mapOf(
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "os" to "Android ${Build.VERSION.RELEASE}",
            "sdk" to Build.VERSION.SDK_INT.toString(),
            "device_id" to Build.ID
        )

        val request = AccessCodeValidationRequest(code, deviceInfo)

        lifecycleScope.launch {
            try {
                val response = app.apiClient.authApi.validateAccessCode(request)

                if (response.isSuccessful && response.body() != null) {
                    val validationResponse = response.body()!!
                    Log.d("MainActivity", "Session ID: ${validationResponse.sessionId}")

                    // Save session data
                    app.sessionManager.saveSession(
                        validationResponse.token,
                        validationResponse.sessionId
                    )

                    // Navigate to camera activity
                    navigateToCameraActivity()
                } else {
                    val errorMessage = when(response.code()) {
                        401 -> "Invalid access code or code already in use"
                        else -> "Server error: ${response.message()}"
                    }
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error: ${e.message}")
                Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
            }
        }
    }

    private fun navigateToCameraActivity() {
        val sessionId = app.sessionManager.getSessionId() ?: return
        val token = app.sessionManager.getToken() ?: return
        
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra("sessionId", sessionId)
            putExtra("token", token)
        }
        startActivity(intent)
        finish()
    }
}