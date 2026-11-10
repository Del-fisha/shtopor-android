package com.company.shtopor.vzvod.data.secure

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Шифрованное хранилище сессии: логин/пароль → token + роль.
 */
class SecureSessionStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "vzvod_secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var baseUrl: String
        get() {
            val stored = prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }
            val fallback = com.company.shtopor.vzvod.BuildConfig.DEFAULT_CORE_BASE_URL
            // Старые локальные/эмуляторные адреса — заменить на публичный домен.
            if (stored != null && (
                    stored.contains("10.0.2.2") ||
                    stored.contains("192.168.0.149") ||
                    stored.contains("127.0.0.1") ||
                    stored.contains("localhost")
                )
            ) {
                prefs.edit().putString(KEY_BASE_URL, fallback).apply()
                return fallback
            }
            return stored ?: fallback
        }
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, null)
            ?: com.company.shtopor.vzvod.BuildConfig.DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var certificatePins: String
        get() = prefs.getString(KEY_PINS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PINS, value).apply()

    var mobileToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        private set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var displayName: String?
        get() = prefs.getString(KEY_DISPLAY_NAME, null)
        set(value) = prefs.edit().putString(KEY_DISPLAY_NAME, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null) ?: prefs.getString(KEY_LAST_USERNAME, null)
        private set(value) {
            val editor = prefs.edit().putString(KEY_USERNAME, value)
            if (!value.isNullOrBlank()) {
                editor.putString(KEY_LAST_USERNAME, value)
            }
            editor.apply()
        }

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var fullAccess: Boolean
        get() = prefs.getBoolean(KEY_FULL_ACCESS, false)
        set(value) = prefs.edit().putBoolean(KEY_FULL_ACCESS, value).apply()

    var post: String?
        get() = prefs.getString(KEY_POST, null)
        set(value) = prefs.edit().putString(KEY_POST, value).apply()

    val isLoggedIn: Boolean
        get() = !mobileToken.isNullOrBlank() && !userId.isNullOrBlank()

    fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, androidId).apply()
        return androidId
    }

    fun saveAuth(
        userId: String,
        displayName: String?,
        username: String?,
        role: String?,
        fullAccess: Boolean,
        token: String
    ) {
        this.userId = userId
        this.displayName = displayName
        this.username = username
        this.role = role
        this.fullAccess = fullAccess
        this.mobileToken = token
    }

    /** Сброс сессии; логин оставляем для удобства формы входа. */
    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_USERNAME)
            .remove(KEY_ROLE)
            .remove(KEY_FULL_ACCESS)
            .remove(KEY_POST)
            .remove(KEY_TOKEN)
            .apply()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PINS = "cert_pins"
        private const val KEY_TOKEN = "mobile_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_LAST_USERNAME = "last_username"
        private const val KEY_ROLE = "role"
        private const val KEY_FULL_ACCESS = "full_access"
        private const val KEY_POST = "post"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
