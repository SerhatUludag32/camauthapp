package com.serhatuludag.camauthapp

import android.app.Application
import com.serhatuludag.camauthapp.api.ApiClient
import com.serhatuludag.camauthapp.data.SessionManager

class AccessCodeApplication : Application() {

    lateinit var sessionManager: SessionManager
    lateinit var apiClient: ApiClient

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager(this)
        apiClient = ApiClient(this)
    }
}