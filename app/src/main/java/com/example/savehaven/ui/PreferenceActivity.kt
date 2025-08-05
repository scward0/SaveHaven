package com.example.savehaven.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.savehaven.R
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PreferencesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var switchWeekly: MaterialSwitch
    private lateinit var switchHabit: MaterialSwitch
    private lateinit var switchDaily: MaterialSwitch
    private lateinit var switchFunFacts: MaterialSwitch
    private lateinit var drawerLayout: DrawerLayout
    private var listenersActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        // Get current user and Firestore
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupNavigationDrawer()
        initViews()

        // Load saved preferences from Firestore
        db.collection("userPreferences").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    switchWeekly.isChecked = document.getBoolean("weekly_summary") ?: true
                    switchHabit.isChecked = document.getBoolean("habit_notifications") ?: true
                    switchDaily.isChecked = document.getBoolean("daily_reminders") ?: true
                    switchFunFacts.isChecked = document.getBoolean("education_facts") ?: true
                }
                // Only activate listeners AFTER setting initial states
                listenersActive = true
                setupSwitchListeners(db, uid)
            }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "User Preferences"
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        // Setup drawer toggle
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            findViewById(R.id.toolbar),
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set navigation item selected listener
        navigationView.setNavigationItemSelectedListener(this)

        // Set Preferences as selected
        navigationView.setCheckedItem(R.id.nav_preferences)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish() // Close this activity when navigating to main screens
            }
            R.id.nav_add_transaction -> {
                startActivity(Intent(this, AddTransactionActivity::class.java))
            }
            R.id.nav_income_overview -> {
                startActivity(Intent(this, IncomeActivity::class.java))
                finish() // Close this activity when navigating to main screens
            }
            R.id.nav_expense_overview -> {
                startActivity(Intent(this, ExpenseActivity::class.java))
                finish() // Close this activity when navigating to main screens
            }
            R.id.nav_find_bank -> {
                startActivity(Intent(this, MapActivity::class.java))
            }
            R.id.nav_preferences -> {
                // Already on this screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset navigation selection to preferences when returning
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setCheckedItem(R.id.nav_preferences)
    }

    private fun initViews() {
        // Link switches to XML
        switchWeekly = findViewById(R.id.switch_weekly_summary)
        switchHabit = findViewById(R.id.switch_habit_notifications)
        switchDaily = findViewById(R.id.switch_daily_reminders)
        switchFunFacts = findViewById(R.id.switch_education_facts)
    }

    private fun setupSwitchListeners(db: FirebaseFirestore, uid: String) {
        // Save when each switch changes
        switchWeekly.setOnCheckedChangeListener { _, isChecked ->
            if (listenersActive) {
                saveToFirestore(db, uid, "weekly_summary", isChecked)
            }
        }

        switchHabit.setOnCheckedChangeListener { _, isChecked ->
            if (listenersActive) {
                saveToFirestore(db, uid, "habit_notifications", isChecked)
            }
        }

        switchDaily.setOnCheckedChangeListener { _, isChecked ->
            if (listenersActive) {
                saveToFirestore(db, uid, "daily_reminders", isChecked)
            }
        }

        switchFunFacts.setOnCheckedChangeListener { _, isChecked ->
            if (listenersActive) {
                saveToFirestore(db, uid, "education_facts", isChecked)
            }
        }
    }

    // Save single key:value to Firestore
    private fun saveToFirestore(db: FirebaseFirestore, uid: String, key: String, value: Boolean) {
        db.collection("userPreferences").document(uid)
            .update(key, value)
            .addOnSuccessListener {
                showToastFor(key, value)
            }
            .addOnFailureListener {
                // Create document if it doesn't exist yet
                db.collection("userPreferences").document(uid)
                    .set(mapOf(key to value))
                    .addOnSuccessListener {
                        showToastFor(key, value)
                    }
            }
    }

    // Toast Messages That Shows Correct Toggling
    private fun showToastFor(key: String, isEnabled: Boolean) {
        val message = when (key) {
            "weekly_summary" -> if (isEnabled) "Weekly summary enabled" else "Weekly summary disabled"
            "habit_notifications" -> if (isEnabled) "Habit change alerts on" else "Habit change alerts off"
            "daily_reminders" -> if (isEnabled) "Daily reminder turned on" else "Daily reminder turned off"
            "education_facts" -> if (isEnabled) "Financial tips enabled" else "Financial tips disabled"
            else -> if (isEnabled) "Setting enabled" else "Setting disabled"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}