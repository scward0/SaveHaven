package com.example.savehaven.utils

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.savehaven.R
import com.example.savehaven.ui.*
import com.example.savehaven.utils.PreferenceHelper
import com.google.firebase.auth.FirebaseAuth

/**
 * Centralized navigation handler - keeps navigation behavior consistent across all activities
 * Every activity with a nav drawer uses this to handle menu clicks
 */
object NavigationHandler {

    /**
     * Handle navigation drawer menu selections
     * shouldFinishOnMainNavigation: true for utility screens, false for main screens
     */
    fun handleNavigation(
        activity: Activity,
        item: MenuItem,
        drawerLayout: DrawerLayout,
        shouldFinishOnMainNavigation: Boolean = true
    ): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Go to dashboard unless we're already there
                if (activity !is DashboardActivity) {
                    activity.startActivity(Intent(activity, DashboardActivity::class.java))
                    if (shouldFinishOnMainNavigation) activity.finish()
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_add_transaction -> {
                // Open add transaction screen
                if (activity !is AddTransactionActivity) {
                    activity.startActivity(Intent(activity, AddTransactionActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_income_overview -> {
                // Show income analysis
                if (activity !is IncomeActivity) {
                    activity.startActivity(Intent(activity, IncomeActivity::class.java))
                    if (shouldFinishOnMainNavigation) activity.finish()
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_expense_overview -> {
                // Show expense analysis
                if (activity !is ExpenseActivity) {
                    activity.startActivity(Intent(activity, ExpenseActivity::class.java))
                    if (shouldFinishOnMainNavigation) activity.finish()
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_transaction_history -> {
                // Show all transactions with filters
                if (activity !is TransactionHistoryActivity) {
                    activity.startActivity(Intent(activity, TransactionHistoryActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_find_bank -> {
                // Open Google Maps bank finder
                if (activity !is MapActivity) {
                    activity.startActivity(Intent(activity, MapActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_preferences -> {
                // Open user settings
                if (activity !is PreferencesActivity) {
                    activity.startActivity(Intent(activity, PreferencesActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_logout -> {
                // Show confirmation dialog before logging out
                showLogoutConfirmation(activity, drawerLayout)
                return true
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Ask user if they really want to logout
    private fun showLogoutConfirmation(activity: Activity, drawerLayout: DrawerLayout) {
        AlertDialog.Builder(activity)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout(activity)
            }
            .setNegativeButton("Cancel") { _, _ ->
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            .show()
    }

    // Actually log the user out and clean up
    private fun performLogout(activity: Activity) {
        // Clear stored user data
        val preferenceHelper = PreferenceHelper(activity)
        preferenceHelper.clearUserSession()

        // Sign out of Firebase
        FirebaseAuth.getInstance().signOut()

        // Go to login screen and clear the activity stack
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}

/**
 * Extension function to highlight the current screen in the nav drawer
 * Call this in onResume() to keep the selection updated
 */
fun setNavigationSelection(activity: Activity, navigationView: com.google.android.material.navigation.NavigationView) {
    when (activity) {
        is DashboardActivity -> navigationView.setCheckedItem(R.id.nav_dashboard)
        is AddTransactionActivity -> navigationView.setCheckedItem(R.id.nav_add_transaction)
        is IncomeActivity -> navigationView.setCheckedItem(R.id.nav_income_overview)
        is ExpenseActivity -> navigationView.setCheckedItem(R.id.nav_expense_overview)
        is TransactionHistoryActivity -> navigationView.setCheckedItem(R.id.nav_transaction_history)
        is MapActivity -> navigationView.setCheckedItem(R.id.nav_find_bank)
        is PreferencesActivity -> navigationView.setCheckedItem(R.id.nav_preferences)
        // EditTransactionActivity doesn't highlight anything (it's like a modal)
    }
}