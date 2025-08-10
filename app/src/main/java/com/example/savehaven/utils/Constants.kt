package com.example.savehaven.utils

/**
 * App constants - all our configuration values in one place
 * Makes it easy to change settings across the entire app
 */
object Constants {
    // Password requirements
    const val MIN_PASSWORD_LENGTH = 8

    // Firestore collection names
    const val COLLECTION_USERS = "users"

    // SharedPreferences keys for "Remember Me" functionality
    const val PREF_REMEMBER_ME = "remember_me"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_NAME = "SaveHaven"
}