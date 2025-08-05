package com.example.savehaven.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.savehaven.R
import com.example.savehaven.data.*
import com.example.savehaven.databinding.ActivityAddTransactionBinding
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var drawerLayout: DrawerLayout
    private var selectedDate = Calendar.getInstance()
    private var isIncome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository()

        setupToolbar()
        setupNavigationDrawer()
        setupUI()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Add Transaction"
    }

    private fun setupNavigationDrawer() {
        drawerLayout = binding.drawerLayout
        val navigationView = binding.navView

        // Setup drawer toggle
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set navigation item selected listener
        navigationView.setNavigationItemSelectedListener(this)

        // Set Add Transaction as selected
        navigationView.setCheckedItem(R.id.nav_add_transaction)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish() // Close this activity when navigating to dashboard
            }
            R.id.nav_add_transaction -> {
                // Already on this screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_income_overview -> {
                startActivity(Intent(this, IncomeActivity::class.java))
            }
            R.id.nav_expense_overview -> {
                startActivity(Intent(this, ExpenseActivity::class.java))
            }
            R.id.nav_find_bank -> {
                startActivity(Intent(this, MapActivity::class.java))
            }
            R.id.nav_preferences -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
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

    private fun setupUI() {
        // Set default date to today
        updateDateDisplay()

        // Setup category dropdown
        updateCategoryDropdown()

        // Default to expense
        binding.btnExpense.isChecked = true
    }

    private fun setupListeners() {
        // Transaction type toggle
        binding.toggleTransactionType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isIncome = checkedId == R.id.btnIncome
                updateCategoryDropdown()
            }
        }

        // Date picker
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Add transaction button
        binding.btnAddTransaction.setOnClickListener {
            addTransaction()
        }
    }

    private fun updateCategoryDropdown() {
        val categories = if (isIncome) {
            TransactionCategories.INCOME_CATEGORIES
        } else {
            TransactionCategories.EXPENSE_CATEGORIES
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
        binding.spinnerCategory.setText("", false)
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.etDate.setText(formatter.format(selectedDate.time))
    }

    private fun addTransaction() {
        // Validate inputs
        val amountText = binding.etAmount.text?.toString()?.trim()
        val category = binding.spinnerCategory.text?.toString()?.trim()
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        if (amountText.isNullOrEmpty()) {
            binding.etAmount.error = "Amount is required"
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAmount.error = "Please enter a valid amount"
            return
        }

        if (category.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Create transaction
        val transaction = Transaction(
            amount = amount,
            category = category,
            description = description,
            date = selectedDate.timeInMillis,
            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        )

        // Save to database
        binding.btnAddTransaction.isEnabled = false
        binding.btnAddTransaction.text = "Adding..."

        transactionRepository.addTransaction(transaction) { success, error ->
            runOnUiThread {
                binding.btnAddTransaction.isEnabled = true
                binding.btnAddTransaction.text = "Add Transaction"

                if (success) {
                    Toast.makeText(this, "Transaction added successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Return to previous screen
                } else {
                    Toast.makeText(this, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}