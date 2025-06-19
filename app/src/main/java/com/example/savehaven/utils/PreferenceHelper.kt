package com.example.savehaven.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

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
}