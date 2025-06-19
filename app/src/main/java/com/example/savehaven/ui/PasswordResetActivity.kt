package com.example.savehaven.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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

class PasswordResetActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnReset: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var tvMessage: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var authRepository: AuthRepository
    private var resetEmailSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset)

        initViews()
        initRepositories()
        setupClickListeners()
        loadPassedEmail()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnReset = findViewById(R.id.btnReset)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        tvMessage = findViewById(R.id.tvMessage)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initRepositories() {
        authRepository = AuthRepository()
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnReset.setOnClickListener { attemptPasswordReset() }
        btnBackToLogin.setOnClickListener { finish() }
    }

    private fun loadPassedEmail() {
        // If email was passed from LoginActivity, pre-fill it
        val passedEmail = intent.getStringExtra("email")
        if (!passedEmail.isNullOrEmpty()) {
            etEmail.setText(passedEmail)
        }
    }

    private fun attemptPasswordReset() {
        val email = etEmail.text.toString().trim()

        // Clear previous errors
        tilEmail.error = null
        tvMessage.visibility = View.GONE

        // Validate email
        if (!validateEmail(email)) {
            return
        }

        // Show loading state
        setLoadingState(true)

        // Perform password reset
        lifecycleScope.launch {
            authRepository.resetPassword(email)
                .onSuccess {
                    setLoadingState(false)
                    showSuccessMessage(email)
                    resetEmailSent = true
                }
                .onFailure { exception ->
                    setLoadingState(false)
                    showErrorMessage(exception.message ?: "Failed to send reset email")
                }
        }
    }

    private fun validateEmail(email: String): Boolean {
        val emailError = ValidationUtils.getEmailErrorMessage(email)
        if (emailError != null) {
            tilEmail.error = emailError
            return false
        }
        return true
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnReset.isEnabled = !isLoading
        btnReset.text = if (isLoading) "" else if (resetEmailSent) "Resend Email" else "Send Reset Email"
        etEmail.isEnabled = !isLoading
    }

    private fun showSuccessMessage(email: String) {
        tvMessage.text = "Password reset instructions have been sent to $email. Please check your inbox and follow the instructions to reset your password."
        tvMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        tvMessage.visibility = View.VISIBLE

        // Update button text for resend functionality
        btnReset.text = "Resend Email"
    }

    private fun showErrorMessage(message: String) {
        tvMessage.text = when {
            message.contains("user") && message.contains("not") ->
                "No account found with this email address. Please check your email or create a new account."
            message.contains("network") || message.contains("internet") ->
                "Network error. Please check your internet connection and try again."
            else -> message
        }
        tvMessage.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        tvMessage.visibility = View.VISIBLE
    }
}