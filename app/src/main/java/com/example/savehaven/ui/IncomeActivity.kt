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
import com.example.savehaven.databinding.ActivityIncomeBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.*

class IncomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomeBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var incomeTransactionAdapter: DashboardTransactionAdapter

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository()

        setupUI()
        setupRecyclerView()
        loadIncomeData()
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
        incomeTransactionAdapter = DashboardTransactionAdapter { transaction ->
            // Navigate to edit transaction
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }

        binding.rvIncomeTransactions.apply {
            layoutManager = LinearLayoutManager(this@IncomeActivity)
            adapter = incomeTransactionAdapter
        }
    }

    private fun loadIncomeData() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    showErrorState()
                } else {
                    val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
                    updateUI(incomeTransactions)
                }
            }
        }
    }

    private fun updateUI(incomeTransactions: List<Transaction>) {
        if (incomeTransactions.isEmpty()) {
            showEmptyState()
        } else {
            showDataState(incomeTransactions)
            updatePieChart(incomeTransactions)
            updateTransactionsList(incomeTransactions)
        }
    }

    private fun updatePieChart(incomeTransactions: List<Transaction>) {
        // Group transactions by category and calculate totals
        val categoryTotals = incomeTransactions
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        if (categoryTotals.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        // Calculate total income for percentages
        val totalIncome = categoryTotals.values.sum()

        // Create pie entries
        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        // Create dataset
        val dataSet = PieDataSet(entries, "Income Categories").apply {
            colors = getIncomeColors()
            valueTextSize = 11f
            valueTextColor = Color.BLACK
            valueFormatter = PercentFormatter(binding.pieChart)
        }

        // Set data to chart
        val data = PieData(dataSet)
        binding.pieChart.data = data

        // Set center text
        binding.pieChart.centerText = "Total Income\n${numberFormat.format(totalIncome)}"
        binding.pieChart.setCenterTextSize(16f)

        binding.pieChart.invalidate()
    }

    private fun updateTransactionsList(incomeTransactions: List<Transaction>) {
        // Sort by date (most recent first) and update adapter
        val sortedTransactions = incomeTransactions.sortedByDescending { it.date }
        incomeTransactionAdapter.updateTransactions(sortedTransactions)

        // Update summary
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val transactionCount = incomeTransactions.size

        binding.tvIncomeSummary.text = "Total: ${numberFormat.format(totalIncome)} • $transactionCount transactions"
    }

    private fun getIncomeColors(): List<Int> {
        return listOf(
            Color.parseColor("#4CAF50"), // Green for Paycheck
            Color.parseColor("#8BC34A"), // Light Green for Gift
            Color.parseColor("#CDDC39")  // Lime for Loan
        )
    }

    private fun showEmptyState() {
        binding.pieChart.visibility = View.GONE
        binding.rvIncomeTransactions.visibility = View.GONE
        binding.tvNoIncome.visibility = View.VISIBLE
        binding.tvIncomeSummary.text = "No income transactions yet"
    }

    private fun showErrorState() {
        binding.pieChart.visibility = View.GONE
        binding.rvIncomeTransactions.visibility = View.GONE
        binding.tvNoIncome.visibility = View.VISIBLE
        binding.tvNoIncome.text = "Error loading income data"
        binding.tvIncomeSummary.text = "Please try again"
    }

    private fun showDataState(incomeTransactions: List<Transaction>) {
        binding.pieChart.visibility = View.VISIBLE
        binding.rvIncomeTransactions.visibility = View.VISIBLE
        binding.tvNoIncome.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadIncomeData() // Refresh data when returning from edit
    }
}