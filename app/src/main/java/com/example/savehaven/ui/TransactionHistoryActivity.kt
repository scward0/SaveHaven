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

class TransactionHistoryActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var rvTransactions: RecyclerView
    private lateinit var spinnerTypeFilter: Spinner
    private lateinit var spinnerCategoryFilter: Spinner
    private lateinit var etDateFrom: EditText
    private lateinit var etDateTo: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnClearFilters: Button
    private lateinit var tvNoTransactions: TextView
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var transactionAdapter: TransactionHistoryAdapter
    private lateinit var transactionRepository: TransactionRepository
    private var allTransactions: List<Transaction> = emptyList()
    private var filteredTransactions: List<Transaction> = emptyList()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        initViews()
        setupToolbar()
        setupNavigationDrawer()
        setupRecyclerView()
        setupFilters()
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

    private fun setupRecyclerView() {
        transactionAdapter = TransactionHistoryAdapter(emptyList()) { transaction ->
            // Handle edit transaction
            editTransaction(transaction)
        }
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = transactionAdapter
    }

    private fun setupFilters() {
        // Type filter
        val typeOptions = listOf("All Types", "Income", "Expense")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeFilter.adapter = typeAdapter

        // Category filter
        updateCategoryFilter()

        // Date filters
        etDateFrom.setOnClickListener { showDatePicker(etDateFrom) }
        etDateTo.setOnClickListener { showDatePicker(etDateTo) }

        // Filter button
        btnFilter.setOnClickListener { applyFilters() }
        btnClearFilters.setOnClickListener { clearFilters() }

        // Type filter change listener
        spinnerTypeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCategoryFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

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

    private fun loadTransactions() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    Toast.makeText(this, "Error loading transactions: $error", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                allTransactions = transactions
                filteredTransactions = transactions
                updateDisplay()
            }
        }
    }

    private fun applyFilters() {
        var filtered = allTransactions

        // Type filter
        val selectedType = spinnerTypeFilter.selectedItem.toString()
        if (selectedType != "All Types") {
            val type = if (selectedType == "Income") TransactionType.INCOME else TransactionType.EXPENSE
            filtered = filtered.filter { it.type == type }
        }

        // Category filter
        val selectedCategory = spinnerCategoryFilter.selectedItem.toString()
        if (selectedCategory != "All Categories") {
            filtered = filtered.filter { it.category == selectedCategory }
        }

        // Date range filter
        val dateFromText = etDateFrom.text.toString()
        val dateToText = etDateTo.text.toString()

        if (dateFromText.isNotEmpty()) {
            try {
                val dateFrom = dateFormat.parse(dateFromText)?.time ?: 0
                filtered = filtered.filter { it.date >= dateFrom }
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid 'From' date format", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (dateToText.isNotEmpty()) {
            try {
                val dateTo = dateFormat.parse(dateToText)?.time ?: Long.MAX_VALUE
                // Add 24 hours to include the entire day
                val dateToEndOfDay = dateTo + (24 * 60 * 60 * 1000)
                filtered = filtered.filter { it.date <= dateToEndOfDay }
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid 'To' date format", Toast.LENGTH_SHORT).show()
                return
            }
        }

        filteredTransactions = filtered
        updateDisplay()
    }

    private fun clearFilters() {
        spinnerTypeFilter.setSelection(0)
        spinnerCategoryFilter.setSelection(0)
        etDateFrom.setText("")
        etDateTo.setText("")
        filteredTransactions = allTransactions
        updateDisplay()
    }

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

    private fun editTransaction(transaction: Transaction) {
        val intent = Intent(this, EditTransactionActivity::class.java)
        intent.putExtra("transaction_id", transaction.id)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions() // Refresh when returning from edit
    }
}