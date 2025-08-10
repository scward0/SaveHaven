package com.example.savehaven.utils

import android.util.Patterns

/**
 * Input validation helpers - keeps validation logic consistent across all forms
 * Returns user-friendly error messages for the UI
 */
object ValidationUtils {

    // Check if email format is valid
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Check if password meets our requirements
    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    // Check if username is acceptable (3-20 chars, letters/numbers/underscore only)
    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank() && username.length >= 3 && username.length <= 20
    }

    // Give user feedback on password strength as they type
    fun getPasswordStrengthMessage(password: String): String {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < Constants.MIN_PASSWORD_LENGTH ->
                "Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters"
            password.length < 10 -> "Good password"
            password.length >= 10 && hasSpecialCharacters(password) -> "Strong password"
            else -> "Good password"
        }
    }

    // Return error message for email field, or null if valid
    fun getEmailErrorMessage(email: String): String? {
        return when {
            email.isEmpty() -> "Email is required"
            !isValidEmail(email) -> "Please enter a valid email address"
            else -> null
        }
    }

    // Return error message for username field, or null if valid
    fun getUsernameErrorMessage(username: String): String? {
        return when {
            username.isEmpty() -> "Username is required"
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 20 -> "Username must be less than 20 characters"
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                "Username can only contain letters, numbers, and underscores"
            else -> null
        }
    }

    // Check if password has good character variety (digit + upper + lower)
    private fun hasSpecialCharacters(password: String): Boolean {
        return password.any { it.isDigit() } &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() }
    }
}