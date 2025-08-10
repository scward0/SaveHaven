package com.example.savehaven.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.savehaven.R
import com.example.savehaven.data.AuthRepository
import com.example.savehaven.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Registration screen with real-time password strength feedback
 * Auto-logs user in after successful registration
 */
class RegisterActivity : AppCompatActivity() {

    // All our form components
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var tvError: TextView
    private lateinit var tvPasswordStrength: TextView // Shows password strength as user types
    private lateinit var progressBar: ProgressBar

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        initRepositories()
        setupClickListeners()
        setupPasswordStrengthIndicator() // Live feedback on password quality
    }

    // Link all UI components
    private fun initViews() {
        tilUsername = findViewById(R.id.tilUsername)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        tvError = findViewById(R.id.tvError)
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initRepositories() {
        authRepository = AuthRepository()
    }

    // Wire up button clicks
    private fun setupClickListeners() {
        btnRegister.setOnClickListener { attemptRegistration() }
        tvLogin.setOnClickListener { goToLogin() }
    }

    // Give user real-time feedback on password strength as they type
    private fun setupPasswordStrengthIndicator() {
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    updatePasswordStrength(password)
                } else {
                    tvPasswordStrength.visibility = View.GONE
                }
            }
        })
    }

    // Update password strength message and color
    private fun updatePasswordStrength(password: String) {
        val strengthMessage = ValidationUtils.getPasswordStrengthMessage(password)
        tvPasswordStrength.text = strengthMessage
        tvPasswordStrength.visibility = View.VISIBLE

        // Color code the strength feedback
        when {
            password.length < 8 -> {
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            password.length < 10 -> {
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            else -> {
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
        }
    }

    // Main registration logic
    private fun attemptRegistration() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Clear any previous errors
        clearErrors()

        // Validate everything before hitting the server
        if (!validateInputs(username, email, password, confirmPassword)) {
            return
        }

        // Show loading state
        setLoadingState(true)

        // Try to create the account
        lifecycleScope.launch {
            authRepository.registerUser(username, email, password)
                .onSuccess { user ->
                    // Registration worked! Go straight to dashboard (auto-login)
                    val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .onFailure { exception ->
                    // Registration failed - show error
                    setLoadingState(false)
                    showError(exception.message ?: "Registration failed")
                }
        }
    }

    // Validate all form inputs
    private fun validateInputs(username: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        // Check username
        val usernameError = ValidationUtils.getUsernameErrorMessage(username)
        if (usernameError != null) {
            tilUsername.error = usernameError
            isValid = false
        }

        // Check email
        val emailError = ValidationUtils.getEmailErrorMessage(email)
        if (emailError != null) {
            tilEmail.error = emailError
            isValid = false
        }

        // Check password strength
        if (!ValidationUtils.isValidPassword(password)) {
            tilPassword.error = "Password must be at least 8 characters"
            isValid = false
        }

        // Check password confirmation
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    // Clear all error messages
    private fun clearErrors() {
        tilUsername.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
        tvError.visibility = View.GONE
    }

    // Show/hide loading spinner and disable inputs
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
        btnRegister.text = if (isLoading) "" else "Create Account"

        // Disable all input fields while processing
        etUsername.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        etConfirmPassword.isEnabled = !isLoading
    }

    // Show error message to user
    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    // Go back to login screen
    private fun goToLogin() {
        finish() // This takes user back to login
    }
}