package com.netconect.app.util

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("netconect_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun saveUserPhotoPath(path: String?) {
        prefs.edit().putString(KEY_USER_PHOTO_PATH, path).apply()
    }

    fun getUserPhotoPath(): String? = prefs.getString(KEY_USER_PHOTO_PATH, null)

    fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url.trimEnd('/')).apply()
    }

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)!!.trimEnd('/')

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://200.106.207.13:27004"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_PHOTO_PATH = "user_photo_path"
        private const val KEY_BASE_URL = "base_url"
    }
}
