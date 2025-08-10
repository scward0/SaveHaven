package com.example.savehaven.ui

import android.app.AlertDialog
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
import com.example.savehaven.databinding.ActivityEditTransactionBinding
import com.example.savehaven.utils.NavigationHandler
import com.example.savehaven.utils.setNavigationSelection
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Edit existing transaction - loads transaction data and allows updates/deletion
 * Similar to AddTransaction but with pre-populated fields and delete functionality
 */
class EditTransactionActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityEditTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var drawerLayout: DrawerLayout
    private var selectedDate = Calendar.getInstance()
    private var isIncome = false
    private var transactionId: String = ""
    private var originalTransaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository()

        // Get the transaction ID from the intent
        transactionId = intent.getStringExtra("transaction_id") ?: ""

        if (transactionId.isEmpty()) {
            Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupNavigationDrawer()
        setupListeners()
        loadTransaction() // Load the transaction data to edit
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Edit Transaction"
    }

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
        // Edit screen acts like a modal, so don't highlight any nav item
        setNavigationSelection(this, navigationView)
    }

    // Finish when navigating to main screens (edit is modal-like)
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

    private fun setupListeners() {
        // Income/expense toggle changes available categories
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

        // Update button
        binding.btnUpdate.setOnClickListener {
            updateTransaction()
        }

        // Delete button with confirmation
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    // Load the transaction data from database
    private fun loadTransaction() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    Toast.makeText(this, "Error loading transaction: $error", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                // Find our transaction in the list
                originalTransaction = transactions.find { it.id == transactionId }
                if (originalTransaction == null) {
                    Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                // Fill in all the form fields with existing data
                populateFields(originalTransaction!!)
            }
        }
    }

    // Fill form fields with existing transaction data
    private fun populateFields(transaction: Transaction) {
        // Set amount (format to 2 decimal places)
        binding.etAmount.setText(String.format("%.2f", transaction.amount))

        // Set description
        binding.etDescription.setText(transaction.description)

        // Set date
        selectedDate.timeInMillis = transaction.date
        updateDateDisplay()

        // Set transaction type (income/expense)
        isIncome = transaction.type == TransactionType.INCOME
        if (isIncome) {
            binding.btnIncome.isChecked = true
        } else {
            binding.btnExpense.isChecked = true
        }

        // Update categories and select the current one
        updateCategoryDropdown()
        binding.spinnerCategory.setText(transaction.category, false)
    }

    // Update category dropdown based on income/expense selection
    private fun updateCategoryDropdown() {
        val categories = if (isIncome) {
            TransactionCategories.INCOME_CATEGORIES
        } else {
            TransactionCategories.EXPENSE_CATEGORIES
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
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

    // Save the updated transaction
    private fun updateTransaction() {
        // Validate all inputs (same as add transaction)
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

        // Create updated transaction (keep original ID and user ID)
        val updatedTransaction = originalTransaction!!.copy(
            amount = amount,
            category = category,
            description = description,
            date = selectedDate.timeInMillis,
            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        )

        // Show loading state
        binding.btnUpdate.isEnabled = false
        binding.btnUpdate.text = "Updating..."

        // Save changes
        transactionRepository.updateTransaction(updatedTransaction) { success, error ->
            runOnUiThread {
                binding.btnUpdate.isEnabled = true
                binding.btnUpdate.text = "Update Transaction"

                if (success) {
                    Toast.makeText(this, "Transaction updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Ask user to confirm deletion
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Actually delete the transaction
    private fun deleteTransaction() {
        binding.btnDelete.isEnabled = false
        binding.btnDelete.text = "Deleting..."

        transactionRepository.deleteTransaction(transactionId) { success, error ->
            runOnUiThread {
                binding.btnDelete.isEnabled = true
                binding.btnDelete.text = "Delete Transaction"

                if (success) {
                    Toast.makeText(this, "Transaction deleted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}