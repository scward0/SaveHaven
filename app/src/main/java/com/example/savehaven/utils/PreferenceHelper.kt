package com.example.savehaven.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PreferenceHelper"
        // Additional keys for session management
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // ========== YOUR EXISTING METHODS ==========

    fun setRememberMe(remember: Boolean, email: String = "") {
        prefs.edit()
            .putBoolean(Constants.PREF_REMEMBER_ME, remember)
            .putString(Constants.PREF_USER_EMAIL, if (remember) email else "")
            .apply()
    }

    fun getRememberedEmail(): String {
        return prefs.getString(Constants.PREF_USER_EMAIL, "") ?: ""
    }

    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(Constants.PREF_REMEMBER_ME, false)
    }

    fun clearSession() {
        prefs.edit()
            .remove(Constants.PREF_REMEMBER_ME)
            .remove(Constants.PREF_USER_EMAIL)
            .apply()
    }

    fun clearUserSession() {
        val editor = prefs.edit()
        editor.clear() // This clears all stored preferences
        editor.apply()
        Log.d(TAG, "User session cleared")
    }

    // ========== ADDITIONAL METHODS FOR DASHBOARD ==========

    /**
     * Save user session information (added for dashboard functionality)
     */
    fun saveUserSession(userId: String, email: String) {
        Log.d(TAG, "Saving user session - UserId: $userId, Email: $email")

        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(Constants.PREF_USER_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }

        Log.d(TAG, "User session saved successfully")
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        val userId = prefs.getString(KEY_USER_ID, null)
        Log.d(TAG, "getCurrentUserId returning: $userId")
        return userId
    }

    /**
     * Get current user email (enhanced version of your existing method)
     */
    fun getCurrentUserEmail(): String? {
        val email = prefs.getString(Constants.PREF_USER_EMAIL, null)
        Log.d(TAG, "getCurrentUserEmail returning: $email")
        return email
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = getCurrentUserId()
        val result = isLoggedIn && !userId.isNullOrEmpty()

        Log.d(TAG, "isUserLoggedIn - IsLoggedIn flag: $isLoggedIn, UserId exists: ${!userId.isNullOrEmpty()}, Result: $result")
        return result
    }

    /**
     * Update user email
     */
    fun updateUserEmail(email: String) {
        Log.d(TAG, "Updating user email: $email")

        prefs.edit().apply {
            putString(Constants.PREF_USER_EMAIL, email)
            apply()
        }

        Log.d(TAG, "User email updated successfully")
    }

    /**
     * Get all stored user info for debugging
     */
    fun getUserInfo(): Map<String, Any?> {
        val userInfo = mapOf(
            "userId" to getCurrentUserId(),
            "email" to getCurrentUserEmail(),
            "isLoggedIn" to isUserLoggedIn(),
            "rememberedEmail" to getRememberedEmail(),
            "rememberMeEnabled" to isRememberMeEnabled()
        )

        Log.d(TAG, "Current user info: $userInfo")
        return userInfo
    }
}