package com.example.savehaven.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.savehaven.R
import com.example.savehaven.data.*
import com.example.savehaven.utils.NavigationHandler
import com.example.savehaven.utils.setNavigationSelection
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction history with advanced filtering
 * Users can filter by type, category, and date range
 */
class TransactionHistoryActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // UI components for filtering
    private lateinit var rvTransactions: RecyclerView
    private lateinit var spinnerTypeFilter: Spinner
    private lateinit var spinnerCategoryFilter: Spinner
    private lateinit var etDateFrom: EditText
    private lateinit var etDateTo: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnClearFilters: Button
    private lateinit var tvNoTransactions: TextView
    private lateinit var drawerLayout: DrawerLayout

    // Data management
    private lateinit var transactionAdapter: TransactionHistoryAdapter
    private lateinit var transactionRepository: TransactionRepository
    private var allTransactions: List<Transaction> = emptyList() // Original data
    private var filteredTransactions: List<Transaction> = emptyList() // What's currently shown

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        initViews()
        setupToolbar()
        setupNavigationDrawer()
        setupRecyclerView()
        setupFilters() // Set up all the filter controls
        transactionRepository = TransactionRepository()

        loadTransactions()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Transaction History"
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            findViewById(R.id.toolbar),
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)
        setNavigationSelection(this, navigationView)
    }

    // This is a utility screen, so finish when going to main navigation
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

    // Link all UI components
    private fun initViews() {
        rvTransactions = findViewById(R.id.rvTransactions)
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter)
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter)
        etDateFrom = findViewById(R.id.etDateFrom)
        etDateTo = findViewById(R.id.etDateTo)
        btnFilter = findViewById(R.id.btnFilter)
        btnClearFilters = findViewById(R.id.btnClearFilters)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)
    }

    // Set up transaction list with click to edit
    private fun setupRecyclerView() {
        transactionAdapter = TransactionHistoryAdapter(emptyList()) { transaction ->
            // When user taps a transaction, open edit screen
            editTransaction(transaction)
        }
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = transactionAdapter
    }

    // Set up all the filter controls and their behavior
    private fun setupFilters() {
        // Type filter dropdown (All, Income, Expense)
        val typeOptions = listOf("All Types", "Income", "Expense")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeFilter.adapter = typeAdapter

        // Category filter (updates based on type selection)
        updateCategoryFilter()

        // Date pickers for from/to dates
        etDateFrom.setOnClickListener { showDatePicker(etDateFrom) }
        etDateTo.setOnClickListener { showDatePicker(etDateTo) }

        // Filter and clear buttons
        btnFilter.setOnClickListener { applyFilters() }
        btnClearFilters.setOnClickListener { clearFilters() }

        // When type changes, update available categories
        spinnerTypeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCategoryFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Update category dropdown based on selected transaction type
    private fun updateCategoryFilter() {
        val selectedType = spinnerTypeFilter.selectedItem.toString()
        val categories = when (selectedType) {
            "Income" -> listOf("All Categories") + TransactionCategories.INCOME_CATEGORIES
            "Expense" -> listOf("All Categories") + TransactionCategories.EXPENSE_CATEGORIES
            else -> listOf("All Categories") + TransactionCategories.INCOME_CATEGORIES + TransactionCategories.EXPENSE_CATEGORIES
        }

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoryFilter.adapter = categoryAdapter
    }

    // Show date picker for from/to date selection
    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                editText.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    // Load all user transactions from database
    private fun loadTransactions() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    Toast.makeText(this, "Error loading transactions: $error", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                // Store original data and show all initially
                allTransactions = transactions
                filteredTransactions = transactions
                updateDisplay()
            }
        }
    }

    // Apply all selected filters to the transaction list
    private fun applyFilters() {
        var filtered = allTransactions

        // Filter by transaction type
        val selectedType = spinnerTypeFilter.selectedItem.toString()
        if (selectedType != "All Types") {
            val type = if (selectedType == "Income") TransactionType.INCOME else TransactionType.EXPENSE
            filtered = filtered.filter { it.type == type }
        }

        // Filter by category
        val selectedCategory = spinnerCategoryFilter.selectedItem.toString()
        if (selectedCategory != "All Categories") {
            filtered = filtered.filter { it.category == selectedCategory }
        }

        // Filter by date range
        val dateFromText = etDateFrom.text.toString()
        val dateToText = etDateTo.text.toString()

        // From date filter
        if (dateFromText.isNotEmpty()) {
            try {
                val dateFrom = dateFormat.parse(dateFromText)?.time ?: 0
                filtered = filtered.filter { it.date >= dateFrom }
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid 'From' date format", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // To date filter (include entire day)
        if (dateToText.isNotEmpty()) {
            try {
                val dateTo = dateFormat.parse(dateToText)?.time ?: Long.MAX_VALUE
                // Add 24 hours to include transactions on the end date
                val dateToEndOfDay = dateTo + (24 * 60 * 60 * 1000)
                filtered = filtered.filter { it.date <= dateToEndOfDay }
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid 'To' date format", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Update display with filtered results
        filteredTransactions = filtered
        updateDisplay()
    }

    // Clear all filters and show all transactions
    private fun clearFilters() {
        spinnerTypeFilter.setSelection(0)
        spinnerCategoryFilter.setSelection(0)
        etDateFrom.setText("")
        etDateTo.setText("")
        filteredTransactions = allTransactions
        updateDisplay()
    }

    // Update the recycler view with current filtered transactions
    private fun updateDisplay() {
        if (filteredTransactions.isEmpty()) {
            rvTransactions.visibility = View.GONE
            tvNoTransactions.visibility = View.VISIBLE
        } else {
            rvTransactions.visibility = View.VISIBLE
            tvNoTransactions.visibility = View.GONE
            transactionAdapter.updateTransactions(filteredTransactions)
        }
    }

    // Open edit screen for selected transaction
    private fun editTransaction(transaction: Transaction) {
        val intent = Intent(this, EditTransactionActivity::class.java)
        intent.putExtra("transaction_id", transaction.id)
        startActivity(intent)
    }

    // Refresh data when returning from edit screen
    override fun onResume() {
        super.onResume()
        loadTransactions()
    }
}