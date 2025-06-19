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
import com.example.savehaven.utils.PreferenceHelper
import com.example.savehaven.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

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

    private lateinit var authRepository: AuthRepository
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        initRepositories()
        setupClickListeners()
        loadRememberedEmail()
    }

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

    private fun initRepositories() {
        authRepository = AuthRepository()
        preferenceHelper = PreferenceHelper(this)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener { attemptLogin() }
        tvCreateAccount.setOnClickListener { goToRegister() }
        tvForgotPassword.setOnClickListener { goToPasswordReset() }
    }

    private fun loadRememberedEmail() {
        if (preferenceHelper.isRememberMeEnabled()) {
            etEmail.setText(preferenceHelper.getRememberedEmail())
            cbRememberMe.isChecked = true
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Clear previous errors
        tilEmail.error = null
        tilPassword.error = null
        tvError.visibility = View.GONE

        // Validate inputs
        if (!validateInputs(email, password)) {
            return
        }

        // Show loading state
        setLoadingState(true)

        // Perform login
        lifecycleScope.launch {
            authRepository.loginUser(email, password)
                .onSuccess { user ->
                    // Handle remember me
                    preferenceHelper.setRememberMe(cbRememberMe.isChecked, email)

                    // Navigate to dashboard
                    val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .onFailure { exception ->
                    setLoadingState(false)
                    showError(exception.message ?: "Login failed")
                }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        // Validate email
        val emailError = ValidationUtils.getEmailErrorMessage(email)
        if (emailError != null) {
            tilEmail.error = emailError
            isValid = false
        }

        // Validate password
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "" else "Login"
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun goToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun goToPasswordReset() {
        val intent = Intent(this, PasswordResetActivity::class.java)
        // Pass the current email if entered
        val email = etEmail.text.toString().trim()
        if (email.isNotEmpty()) {
            intent.putExtra("email", email)
        }
        startActivity(intent)
    }
}