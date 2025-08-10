package com.example.savehaven.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.savehaven.R
import com.example.savehaven.data.Transaction
import com.example.savehaven.data.TransactionRepository
import com.example.savehaven.data.TransactionType
import com.example.savehaven.databinding.ActivityExpenseBinding
import com.example.savehaven.utils.NavigationHandler
import com.example.savehaven.utils.setNavigationSelection
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.navigation.NavigationView
import java.text.NumberFormat
import java.util.*

/**
 * Expense analysis screen - similar to IncomeActivity but for expenses
 * Shows pie chart breakdown and list of all expense transactions
 */
class ExpenseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityExpenseBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var expenseTransactionAdapter: DashboardTransactionAdapter
    private lateinit var drawerLayout: DrawerLayout

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository()

        setupToolbar()
        setupNavigationDrawer()
        setupUI()
        setupRecyclerView()
        loadExpenseData() // Load and display expense data
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Expense Overview"
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
        setNavigationSelection(this, navigationView)
    }

    // Finish when going to main navigation (this is an overview screen)
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

    // Refresh data when returning from edit transaction
    override fun onResume() {
        super.onResume()
        loadExpenseData()
        setNavigationSelection(this, binding.navView)
    }

    private fun setupUI() {
        // Configure the pie chart (same setup as income chart)
        setupPieChart()
    }

    // Set up pie chart styling - same as income but with red color scheme
    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f

            // Center hole configuration
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)

            // Interaction and animation
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)

            // Labels
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }
    }

    // Set up recycler view for expense transaction list
    private fun setupRecyclerView() {
        expenseTransactionAdapter = DashboardTransactionAdapter { transaction ->
            // Navigate to edit when user taps a transaction
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }

        binding.rvExpenseTransactions.apply {
            layoutManager = LinearLayoutManager(this@ExpenseActivity)
            adapter = expenseTransactionAdapter
        }
    }

    // Load all expense transactions and update UI
    private fun loadExpenseData() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    showErrorState()
                } else {
                    // Filter to just expense transactions
                    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
                    updateUI(expenseTransactions)
                }
            }
        }
    }

    // Update all UI components with expense data
    private fun updateUI(expenseTransactions: List<Transaction>) {
        if (expenseTransactions.isEmpty()) {
            showEmptyState()
        } else {
            showDataState(expenseTransactions)
            updatePieChart(expenseTransactions)
            updateTransactionsList(expenseTransactions)
        }
    }

    // Create pie chart from expense data
    private fun updatePieChart(expenseTransactions: List<Transaction>) {
        // Group by category and sum amounts
        val categoryTotals = expenseTransactions
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        if (categoryTotals.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val totalExpenses = categoryTotals.values.sum()

        // Create pie entries
        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        // Style with red color scheme for expenses
        val dataSet = PieDataSet(entries, "Expense Categories").apply {
            colors = getExpenseColors() // Red/pink color scheme for expenses
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            valueFormatter = PercentFormatter(binding.pieChart)
        }

        // Apply to chart
        val data = PieData(dataSet)
        binding.pieChart.data = data

        // Show total expenses in center
        binding.pieChart.centerText = "Total Expenses\n${numberFormat.format(totalExpenses)}"
        binding.pieChart.setCenterTextSize(16f)

        binding.pieChart.invalidate()
    }

    // Update transaction list and summary
    private fun updateTransactionsList(expenseTransactions: List<Transaction>) {
        // Sort newest first
        val sortedTransactions = expenseTransactions.sortedByDescending { it.date }
        expenseTransactionAdapter.updateTransactions(sortedTransactions)

        // Update summary text
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        val transactionCount = expenseTransactions.size

        binding.tvExpenseSummary.text = "Total: ${numberFormat.format(totalExpenses)} • $transactionCount transactions"
    }

    // Red/pink color scheme for expense categories
    private fun getExpenseColors(): List<Int> {
        return listOf(
            Color.parseColor("#F44336"), // Red for Food
            Color.parseColor("#E91E63"), // Pink for Entertainment
            Color.parseColor("#9C27B0"), // Purple for Rent
            Color.parseColor("#FF5722"), // Deep Orange for Car
            Color.parseColor("#795548")  // Brown for Miscellaneous
        )
    }

    // Show when user has no expense transactions
    private fun showEmptyState() {
        binding.pieChart.visibility = View.GONE
        binding.rvExpenseTransactions.visibility = View.GONE
        binding.tvNoExpenses.visibility = View.VISIBLE
        binding.tvExpenseSummary.text = "No expense transactions yet"
    }

    // Show error message
    private fun showErrorState() {
        binding.pieChart.visibility = View.GONE
        binding.rvExpenseTransactions.visibility = View.GONE
        binding.tvNoExpenses.visibility = View.VISIBLE
        binding.tvNoExpenses.text = "Error loading expense data"
        binding.tvExpenseSummary.text = "Please try again"
    }

    // Show when we have expense data
    private fun showDataState(expenseTransactions: List<Transaction>) {
        binding.pieChart.visibility = View.VISIBLE
        binding.rvExpenseTransactions.visibility = View.VISIBLE
        binding.tvNoExpenses.visibility = View.GONE
    }
}