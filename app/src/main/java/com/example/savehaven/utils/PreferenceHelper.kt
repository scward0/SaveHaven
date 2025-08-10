package com.example.savehaven.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

/**
 * Helper class for managing user preferences and session data
 * Handles both login persistence and app settings like notification preferences
 */
class PreferenceHelper(context: Context) {
    // Main preferences file for login/session data
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    // Separate preferences file for app settings (like notification toggles)
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SaveHavenPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PreferenceHelper"
        // Session management keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Generic boolean getter for app settings (education tips, notifications, etc.)
    fun getBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    // Generic boolean setter for app settings
    fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    // Save "remember me" choice and optionally the email
    fun setRememberMe(remember: Boolean, email: String = "") {
        prefs.edit {
            putBoolean(Constants.PREF_REMEMBER_ME, remember)
                .putString(Constants.PREF_USER_EMAIL, if (remember) email else "")
        }
    }

    // Get the email we saved for "remember me" functionality
    fun getRememberedEmail(): String {
        return prefs.getString(Constants.PREF_USER_EMAIL, "") ?: ""
    }

    // Check if user wants to be remembered
    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(Constants.PREF_REMEMBER_ME, false)
    }

    // Clear just the remember me data (partial logout)
    fun clearSession() {
        prefs.edit()
            .remove(Constants.PREF_REMEMBER_ME)
            .remove(Constants.PREF_USER_EMAIL)
            .apply()
    }

    // Nuclear option - clear ALL preferences (full logout)
    fun clearUserSession() {
        val editor = prefs.edit()
        editor.clear() // Wipe everything
        editor.apply()
    }

    // Save complete user session info (used when login succeeds)
    fun saveUserSession(userId: String, email: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(Constants.PREF_USER_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    // Get the current user's Firebase UID
    fun getCurrentUserId(): String? {
        val userId = prefs.getString(KEY_USER_ID, null)
        return userId
    }

    // Get current user's email (enhanced version that's more reliable)
    fun getCurrentUserEmail(): String? {
        val email = prefs.getString(Constants.PREF_USER_EMAIL, null)
        return email
    }

    // Check if user is properly logged in (has both flag and user ID)
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = getCurrentUserId()
        val result = isLoggedIn && !userId.isNullOrEmpty()
        return result
    }

    // Update just the email (for profile updates)
    fun updateUserEmail(email: String) {
        prefs.edit().apply {
            putString(Constants.PREF_USER_EMAIL, email)
            apply()
        }
    }

    // Debug helper - dump all user-related preferences
    fun getUserInfo(): Map<String, Any?> {
        val userInfo = mapOf(
            "userId" to getCurrentUserId(),
            "email" to getCurrentUserEmail(),
            "isLoggedIn" to isUserLoggedIn(),
            "rememberedEmail" to getRememberedEmail(),
            "rememberMeEnabled" to isRememberMeEnabled()
        )
        return userInfo
    }
}