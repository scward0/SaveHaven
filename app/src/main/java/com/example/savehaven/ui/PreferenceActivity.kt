package com.example.savehaven.ui

import android.widget.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.savehaven.*
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PreferencesActivity : AppCompatActivity() {

    private lateinit var switchWeekly: MaterialSwitch
    private lateinit var switchHabit: MaterialSwitch
    private lateinit var switchDaily: MaterialSwitch
    private lateinit var switchFunFacts: MaterialSwitch
    private var listenersActive = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        // Get current user and Firestore
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Link switches to XML
        switchWeekly = findViewById(R.id.switch_weekly_summary)
        switchHabit = findViewById(R.id.switch_habit_notifications)
        switchDaily = findViewById(R.id.switch_daily_reminders)
        switchFunFacts = findViewById(R.id.switch_education_facts)

        // Closes PreferencesActivity & Returns to Dashboard
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

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
            }
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
