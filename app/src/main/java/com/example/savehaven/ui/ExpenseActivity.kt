package com.example.savehaven.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.savehaven.R
import com.example.savehaven.data.Transaction
import com.example.savehaven.data.TransactionRepository
import com.example.savehaven.data.TransactionType
import com.example.savehaven.databinding.ActivityExpenseBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.*

class ExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var expenseTransactionAdapter: DashboardTransactionAdapter

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository()

        setupUI()
        setupRecyclerView()
        loadExpenseData()
    }

    private fun setupUI() {
        // Set up toolbar/back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Initialize chart
        setupPieChart()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }
    }

    private fun setupRecyclerView() {
        expenseTransactionAdapter = DashboardTransactionAdapter { transaction ->
            // Navigate to edit transaction
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }

        binding.rvExpenseTransactions.apply {
            layoutManager = LinearLayoutManager(this@ExpenseActivity)
            adapter = expenseTransactionAdapter
        }
    }

    private fun loadExpenseData() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    showErrorState()
                } else {
                    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
                    updateUI(expenseTransactions)
                }
            }
        }
    }

    private fun updateUI(expenseTransactions: List<Transaction>) {
        if (expenseTransactions.isEmpty()) {
            showEmptyState()
        } else {
            showDataState(expenseTransactions)
            updatePieChart(expenseTransactions)
            updateTransactionsList(expenseTransactions)
        }
    }

    private fun updatePieChart(expenseTransactions: List<Transaction>) {
        // Group transactions by category and calculate totals
        val categoryTotals = expenseTransactions
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        if (categoryTotals.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        // Calculate total expenses for percentages
        val totalExpenses = categoryTotals.values.sum()

        // Create pie entries
        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        // Create dataset
        val dataSet = PieDataSet(entries, "Expense Categories").apply {
            colors = getExpenseColors()
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            valueFormatter = PercentFormatter(binding.pieChart)
        }

        // Set data to chart
        val data = PieData(dataSet)
        binding.pieChart.data = data

        // Set center text
        binding.pieChart.centerText = "Total Expenses\n${numberFormat.format(totalExpenses)}"
        binding.pieChart.setCenterTextSize(16f)

        binding.pieChart.invalidate()
    }

    private fun updateTransactionsList(expenseTransactions: List<Transaction>) {
        // Sort by date (most recent first) and update adapter
        val sortedTransactions = expenseTransactions.sortedByDescending { it.date }
        expenseTransactionAdapter.updateTransactions(sortedTransactions)

        // Update summary
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        val transactionCount = expenseTransactions.size

        binding.tvExpenseSummary.text = "Total: ${numberFormat.format(totalExpenses)} • $transactionCount transactions"
    }

    private fun getExpenseColors(): List<Int> {
        return listOf(
            Color.parseColor("#F44336"), // Red for Food
            Color.parseColor("#E91E63"), // Pink for Entertainment
            Color.parseColor("#9C27B0"), // Purple for Rent
            Color.parseColor("#FF5722"), // Deep Orange for Car
            Color.parseColor("#795548")  // Brown for Miscellaneous
        )
    }

    private fun showEmptyState() {
        binding.pieChart.visibility = View.GONE
        binding.rvExpenseTransactions.visibility = View.GONE
        binding.tvNoExpenses.visibility = View.VISIBLE
        binding.tvExpenseSummary.text = "No expense transactions yet"
    }

    private fun showErrorState() {
        binding.pieChart.visibility = View.GONE
        binding.rvExpenseTransactions.visibility = View.GONE
        binding.tvNoExpenses.visibility = View.VISIBLE
        binding.tvNoExpenses.text = "Error loading expense data"
        binding.tvExpenseSummary.text = "Please try again"
    }

    private fun showDataState(expenseTransactions: List<Transaction>) {
        binding.pieChart.visibility = View.VISIBLE
        binding.rvExpenseTransactions.visibility = View.VISIBLE
        binding.tvNoExpenses.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadExpenseData() // Refresh data when returning from edit
    }
}