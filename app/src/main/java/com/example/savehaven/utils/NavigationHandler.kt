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

object NavigationHandler {

    /**
     * Universal navigation handler for all activities with navigation drawer
     * @param activity The current activity
     * @param item The selected menu item
     * @param drawerLayout The drawer layout to close
     * @param shouldFinishOnMainNavigation Whether to finish current activity when navigating to main screens
     * @return true if navigation was handled
     */
    fun handleNavigation(
        activity: Activity,
        item: MenuItem,
        drawerLayout: DrawerLayout,
        shouldFinishOnMainNavigation: Boolean = true
    ): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                if (activity !is DashboardActivity) {
                    activity.startActivity(Intent(activity, DashboardActivity::class.java))
                    if (shouldFinishOnMainNavigation) activity.finish()
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_add_transaction -> {
                if (activity !is AddTransactionActivity) {
                    activity.startActivity(Intent(activity, AddTransactionActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_income_overview -> {
                if (activity !is IncomeActivity) {
                    activity.startActivity(Intent(activity, IncomeActivity::class.java))
                    if (shouldFinishOnMainNavigation) activity.finish()
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_expense_overview -> {
                if (activity !is ExpenseActivity) {
                    activity.startActivity(Intent(activity, ExpenseActivity::class.java))
                    if (shouldFinishOnMainNavigation) activity.finish()
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_transaction_history -> {
                if (activity !is TransactionHistoryActivity) {
                    activity.startActivity(Intent(activity, TransactionHistoryActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_find_bank -> {
                if (activity !is MapActivity) {
                    activity.startActivity(Intent(activity, MapActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_preferences -> {
                if (activity !is PreferencesActivity) {
                    activity.startActivity(Intent(activity, PreferencesActivity::class.java))
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
            R.id.nav_logout -> {
                showLogoutConfirmation(activity, drawerLayout)
                return true
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

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

    private fun performLogout(activity: Activity) {
        // Clear user session
        val preferenceHelper = PreferenceHelper(activity)
        preferenceHelper.clearUserSession()

        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Navigate to login
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}

/**
 * Extension function to set the correct navigation item as checked
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
        // EditTransactionActivity doesn't set any selection (modal-like behavior)
    }
}