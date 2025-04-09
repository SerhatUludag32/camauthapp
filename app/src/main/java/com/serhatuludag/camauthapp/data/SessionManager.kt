package com.serhatuludag.camauthapp.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        private const val PREFS_NAME = "CamAuthAppPrefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_SESSION_ID = "session_id"
    }

    fun saveSession(token: String, sessionId: String) {
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_SESSION_ID, sessionId)
        editor.apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getSessionId(): String? {
        return prefs.getString(KEY_SESSION_ID, null)
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null && getSessionId() != null
    }

    fun clearSession() {
        editor.clear()
        editor.apply()
    }
}