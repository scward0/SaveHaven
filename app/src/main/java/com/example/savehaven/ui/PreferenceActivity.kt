package com.example.savehaven.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.savehaven.R
import com.example.savehaven.utils.NavigationHandler
import com.example.savehaven.utils.PreferenceHelper
import com.example.savehaven.utils.setNavigationSelection
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

                    val educationEnabled = document.getBoolean("education_facts") ?: true
                    switchFunFacts.isChecked = educationEnabled

                    // Sync local preference
                    PreferenceHelper(this).setBoolean("education_facts", educationEnabled)

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

        // Use the extension function to set the correct selection
        setNavigationSelection(this, navigationView)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Use the universal NavigationHandler - finish on main navigation since this is a utility screen
        return NavigationHandler.handleNavigation(this, item, drawerLayout, shouldFinishOnMainNavigation = true)
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
        // Reset navigation selection when returning
        setNavigationSelection(this, findViewById<NavigationView>(R.id.nav_view))
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
                // ALSO update local SharedPreferences so MainActivity & DashboardActivity read correct value
                PreferenceHelper(this).setBoolean("education_facts", isChecked)
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