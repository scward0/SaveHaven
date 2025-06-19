package com.example.savehaven

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.savehaven.ui.LoginActivity
import com.example.savehaven.ui.DashboardActivity
import com.example.savehaven.utils.PreferenceHelper

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        preferenceHelper = PreferenceHelper(this)

        // Show splash screen for 2 seconds, then check authentication
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, 2000)
    }

    private fun checkAuthenticationStatus() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is signed in, go to dashboard
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            // User is not signed in, go to login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish() // Close MainActivity so user can't go back to it
    }
}