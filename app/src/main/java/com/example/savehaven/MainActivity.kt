package com.example.savehaven

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.savehaven.ui.LoginActivity
import com.example.savehaven.ui.DashboardActivity
import com.example.savehaven.utils.FinancialTipsProvider
import com.example.savehaven.utils.PreferenceHelper

/**
 * Main entry point - handles splash screen and user routing
 * Shows splash for 2 seconds, then sends users to Dashboard or Login
 */
class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up our Firebase auth and preferences
        auth = FirebaseAuth.getInstance()
        preferenceHelper = PreferenceHelper(this)

        // Wait 2 seconds for splash effect, then check if user is logged in
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, 2000)
    }

    private fun checkAuthenticationStatus() {
        val currentUser = auth.currentUser
        val showTips = preferenceHelper.getBoolean("education_facts", true)

        if (currentUser != null) {
            // User is already logged in - go straight to dashboard
            val intent = Intent(this, DashboardActivity::class.java)

            // If they like tips, grab a random one to show them
            if (showTips) {
                val tip = FinancialTipsProvider.getRandomTip()
                intent.putExtra("financial_tip", tip)
            }

            startActivity(intent)
        } else {
            // No user logged in - send them to login screen
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Close this activity so they can't go back to splash screen
        finish()
    }
}