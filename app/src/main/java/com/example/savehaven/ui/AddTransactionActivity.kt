package com.example.savehaven.ui

import android.app.DatePickerDialog
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
import com.example.savehaven.utils.NavigationHandler
import com.example.savehaven.utils.setNavigationSelection
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Add new transaction screen with smart category switching
 * Categories change based on income vs expense selection
 */
class AddTransactionActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var drawerLayout: DrawerLayout
    private var selectedDate = Calendar.getInstance() // Defaults to today
    private var isIncome = false // Track whether user selected income or expense

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

    // Standard nav drawer setup - same pattern across the app
    private fun setupNavigationDrawer() {
        drawerLayout = binding.drawerLayout
        val navigationView = binding.navView

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)
        setNavigationSelection(this, navigationView)
    }

    // Use the centralized navigation handler, finish this activity when going to main screens
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return NavigationHandler.handleNavigation(this, item, drawerLayout, shouldFinishOnMainNavigation = true)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Set up initial UI state
    private fun setupUI() {
        // Show today's date by default
        updateDateDisplay()

        // Set up category dropdown (will be empty until user picks income/expense)
        updateCategoryDropdown()

        // Default to expense (most common transaction type)
        binding.btnExpense.isChecked = true
    }

    // Wire up all the interactive elements
    private fun setupListeners() {
        // When user switches between income/expense, update category options
        binding.toggleTransactionType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isIncome = checkedId == R.id.btnIncome
                updateCategoryDropdown() // Refresh categories based on selection
            }
        }

        // Date picker when user taps date field
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Main save button
        binding.btnAddTransaction.setOnClickListener {
            addTransaction()
        }
    }

    // Update category dropdown based on income vs expense selection
    private fun updateCategoryDropdown() {
        val categories = if (isIncome) {
            TransactionCategories.INCOME_CATEGORIES
        } else {
            TransactionCategories.EXPENSE_CATEGORIES
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
        binding.spinnerCategory.setText("", false) // Clear current selection
    }

    // Show date picker dialog
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

    // Update the date field display
    private fun updateDateDisplay() {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.etDate.setText(formatter.format(selectedDate.time))
    }

    // Main transaction creation logic
    private fun addTransaction() {
        // Get all the form values
        val amountText = binding.etAmount.text?.toString()?.trim()
        val category = binding.spinnerCategory.text?.toString()?.trim()
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        // Validate amount
        if (amountText.isNullOrEmpty()) {
            binding.etAmount.error = "Amount is required"
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAmount.error = "Please enter a valid amount"
            return
        }

        // Validate category selection
        if (category.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the transaction object
        val transaction = Transaction(
            amount = amount,
            category = category,
            description = description,
            date = selectedDate.timeInMillis,
            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        )

        // Show loading state while saving
        binding.btnAddTransaction.isEnabled = false
        binding.btnAddTransaction.text = "Adding..."

        // Save to database
        transactionRepository.addTransaction(transaction) { success, error ->
            runOnUiThread {
                // Reset button state
                binding.btnAddTransaction.isEnabled = true
                binding.btnAddTransaction.text = "Add Transaction"

                if (success) {
                    Toast.makeText(this, "Transaction added successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to previous screen
                } else {
                    Toast.makeText(this, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}