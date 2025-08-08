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
        transactionId = intent.getStringExtra("transaction_id") ?: ""

        if (transactionId.isEmpty()) {
            Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupNavigationDrawer()
        setupListeners()
        loadTransaction()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Edit Transaction"
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

        // Don't set any selection for EditTransactionActivity (modal-like behavior)
        // The setNavigationSelection extension function handles this automatically
        setNavigationSelection(this, navigationView)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Use the universal NavigationHandler - finish on main navigation since this is a modal-like edit screen
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

        // Update button
        binding.btnUpdate.setOnClickListener {
            updateTransaction()
        }

        // Delete button
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadTransaction() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    Toast.makeText(this, "Error loading transaction: $error", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                originalTransaction = transactions.find { it.id == transactionId }
                if (originalTransaction == null) {
                    Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                populateFields(originalTransaction!!)
            }
        }
    }

    private fun populateFields(transaction: Transaction) {
        // Set amount
        binding.etAmount.setText(String.format("%.2f", transaction.amount))

        // Set description
        binding.etDescription.setText(transaction.description)

        // Set date
        selectedDate.timeInMillis = transaction.date
        updateDateDisplay()

        // Set transaction type
        isIncome = transaction.type == TransactionType.INCOME
        if (isIncome) {
            binding.btnIncome.isChecked = true
        } else {
            binding.btnExpense.isChecked = true
        }

        // Update category dropdown and set selected category
        updateCategoryDropdown()
        binding.spinnerCategory.setText(transaction.category, false)
    }

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

    private fun updateTransaction() {
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

        // Create updated transaction
        val updatedTransaction = originalTransaction!!.copy(
            amount = amount,
            category = category,
            description = description,
            date = selectedDate.timeInMillis,
            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        )

        // Save to database
        binding.btnUpdate.isEnabled = false
        binding.btnUpdate.text = "Updating..."

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