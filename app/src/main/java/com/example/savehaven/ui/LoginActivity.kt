package com.example.savehaven.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.savehaven.R
import com.example.savehaven.data.AuthRepository
import com.example.savehaven.utils.FinancialTipsProvider
import com.example.savehaven.utils.PreferenceHelper
import com.example.savehaven.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Login screen - handles user authentication and "remember me" functionality
 * Also triggers educational tips if user has them enabled
 */
class LoginActivity : AppCompatActivity() {

    // UI components - using findViewById since this was built before view binding everywhere
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var cbRememberMe: CheckBox
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvCreateAccount: TextView
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    // Our data repositories
    private lateinit var authRepository: AuthRepository
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        initRepositories()
        setupClickListeners()
        loadRememberedEmail() // Pre-fill email if user chose "remember me" last time
    }

    // Link all our UI components
    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)
    }

    // Set up our data access objects
    private fun initRepositories() {
        authRepository = AuthRepository()
        preferenceHelper = PreferenceHelper(this)
    }

    // Wire up all the button clicks
    private fun setupClickListeners() {
        btnLogin.setOnClickListener { attemptLogin() }
        tvCreateAccount.setOnClickListener { goToRegister() }
        tvForgotPassword.setOnClickListener { goToPasswordReset() }
    }

    // If user checked "remember me" before, pre-fill their email
    private fun loadRememberedEmail() {
        if (preferenceHelper.isRememberMeEnabled()) {
            etEmail.setText(preferenceHelper.getRememberedEmail())
            cbRememberMe.isChecked = true
        }
    }

    // Main login logic
    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Clear any previous error messages
        tilEmail.error = null
        tilPassword.error = null
        tvError.visibility = View.GONE

        // Check if inputs are valid before hitting the server
        if (!validateInputs(email, password)) {
            return
        }

        // Show loading spinner
        setLoadingState(true)

        // Use coroutine for async Firebase auth
        lifecycleScope.launch {
            authRepository.loginUser(email, password)
                .onSuccess { user ->
                    // Login worked! Handle remember me preference
                    preferenceHelper.setRememberMe(cbRememberMe.isChecked, email)

                    // Go to dashboard, maybe with a financial tip
                    val showTips = preferenceHelper.getBoolean("education_facts", true)
                    val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                    if (showTips) {
                        val tip = FinancialTipsProvider.getRandomTip()
                        intent.putExtra("financial_tip", tip)
                    }

                    startActivity(intent)
                    finish()
                }
                .onFailure { exception ->
                    // Login failed - show error and hide loading
                    setLoadingState(false)
                    showError(exception.message ?: "Login failed")
                }
        }
    }

    // Basic input validation before trying to login
    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        // Check email format
        val emailError = ValidationUtils.getEmailErrorMessage(email)
        if (emailError != null) {
            tilEmail.error = emailError
            isValid = false
        }

        // Check password isn't empty
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    // Show/hide loading spinner and disable button
    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "" else "Login"
    }

    // Display error message to user
    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    // Navigate to registration screen
    private fun goToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    // Navigate to password reset screen
    private fun goToPasswordReset() {
        val intent = Intent(this, PasswordResetActivity::class.java)
        // Pass current email if they've entered one
        val email = etEmail.text.toString().trim()
        if (email.isNotEmpty()) {
            intent.putExtra("email", email)
        }
        startActivity(intent)
    }
}