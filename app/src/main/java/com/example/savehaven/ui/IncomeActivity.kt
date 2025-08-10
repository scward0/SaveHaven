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
import com.example.savehaven.databinding.ActivityIncomeBinding
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
 * Income analysis screen - shows pie chart breakdown and list of all income transactions
 * Uses MPAndroidChart for the pie chart visualization
 */
class IncomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityIncomeBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var incomeTransactionAdapter: DashboardTransactionAdapter
    private lateinit var drawerLayout: DrawerLayout

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository()

        setupToolbar()
        setupNavigationDrawer()
        setupUI()
        setupRecyclerView()
        loadIncomeData() // Load and display income data
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Income Overview"
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
        loadIncomeData()
        setNavigationSelection(this, binding.navView)
    }

    private fun setupUI() {
        // Configure the pie chart appearance
        setupPieChart()
    }

    // Set up pie chart styling and behavior
    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true) // Show percentages instead of raw values
            description.isEnabled = false // Hide chart description
            setExtraOffsets(5f, 10f, 5f, 5f) // Add padding around chart
            dragDecelerationFrictionCoef = 0.95f // Smooth spinning

            // Configure the center hole
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true) // We'll put total income in center

            // Interaction settings
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400) // Animate chart appearance

            // Labels and legend
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }
    }

    // Set up recycler view for income transaction list
    private fun setupRecyclerView() {
        incomeTransactionAdapter = DashboardTransactionAdapter { transaction ->
            // When user taps a transaction, open edit screen
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }

        binding.rvIncomeTransactions.apply {
            layoutManager = LinearLayoutManager(this@IncomeActivity)
            adapter = incomeTransactionAdapter
        }
    }

    // Load all income transactions and update UI
    private fun loadIncomeData() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    showErrorState()
                } else {
                    // Filter to just income transactions
                    val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
                    updateUI(incomeTransactions)
                }
            }
        }
    }

    // Update all UI components with income data
    private fun updateUI(incomeTransactions: List<Transaction>) {
        if (incomeTransactions.isEmpty()) {
            showEmptyState()
        } else {
            showDataState(incomeTransactions)
            updatePieChart(incomeTransactions)
            updateTransactionsList(incomeTransactions)
        }
    }

    // Create pie chart from income data
    private fun updatePieChart(incomeTransactions: List<Transaction>) {
        // Group transactions by category and sum amounts
        val categoryTotals = incomeTransactions
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        if (categoryTotals.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val totalIncome = categoryTotals.values.sum()

        // Create pie chart entries
        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        // Style the chart
        val dataSet = PieDataSet(entries, "Income Categories").apply {
            colors = getIncomeColors() // Green color scheme for income
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            valueFormatter = PercentFormatter(binding.pieChart)
        }

        // Apply data to chart
        val data = PieData(dataSet)
        binding.pieChart.data = data

        // Show total in center of donut chart
        binding.pieChart.centerText = "Total Income\n${numberFormat.format(totalIncome)}"
        binding.pieChart.setCenterTextSize(16f)

        binding.pieChart.invalidate() // Refresh chart
    }

    // Update the transaction list below the chart
    private fun updateTransactionsList(incomeTransactions: List<Transaction>) {
        // Sort newest first for better UX
        val sortedTransactions = incomeTransactions.sortedByDescending { it.date }
        incomeTransactionAdapter.updateTransactions(sortedTransactions)

        // Update summary text
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val transactionCount = incomeTransactions.size

        binding.tvIncomeSummary.text = "Total: ${numberFormat.format(totalIncome)} • $transactionCount transactions"
    }

    // Green color scheme for income categories
    private fun getIncomeColors(): List<Int> {
        return listOf(
            Color.parseColor("#4CAF50"), // Green for Paycheck
            Color.parseColor("#8BC34A"), // Light Green for Gift
            Color.parseColor("#CDDC39")  // Lime for Loan
        )
    }

    // Show when user has no income transactions
    private fun showEmptyState() {
        binding.pieChart.visibility = View.GONE
        binding.rvIncomeTransactions.visibility = View.GONE
        binding.tvNoIncome.visibility = View.VISIBLE
        binding.tvIncomeSummary.text = "No income transactions yet"
    }

    // Show when there's an error loading data
    private fun showErrorState() {
        binding.pieChart.visibility = View.GONE
        binding.rvIncomeTransactions.visibility = View.GONE
        binding.tvNoIncome.visibility = View.VISIBLE
        binding.tvNoIncome.text = "Error loading income data"
        binding.tvIncomeSummary.text = "Please try again"
    }

    // Show when we have income data to display
    private fun showDataState(incomeTransactions: List<Transaction>) {
        binding.pieChart.visibility = View.VISIBLE
        binding.rvIncomeTransactions.visibility = View.VISIBLE
        binding.tvNoIncome.visibility = View.GONE
    }
}