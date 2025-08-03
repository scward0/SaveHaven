package com.example.savehaven.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.savehaven.data.*
import com.example.savehaven.databinding.ActivityDashboardBinding
import com.example.savehaven.utils.PreferenceHelper
import com.example.savehaven.*
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var recentTransactionsAdapter: DashboardTransactionAdapter

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize dependencies
        transactionRepository = TransactionRepository()
        preferenceHelper = PreferenceHelper(this)

        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // Load data
        loadData()

        // SETTINGS BUTTON LOGIC RIGHT HERE
        val settingsButton = findViewById<ImageButton>(R.id.settings_icon)
        settingsButton.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }

        binding.btnOpenMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        loadTransactions()
    }

    private fun loadTransactions() {
        transactionRepository.getUserTransactions { transactions, error ->
            runOnUiThread {
                if (error != null) {
                    showDefaultState()
                } else {
                    updateUI(transactions)
                }
            }
        }
    }

    private fun updateUI(transactions: List<Transaction>) {
        updateFinancialSummary(transactions)
        updateRecentTransactions(transactions)
    }

    private fun updateFinancialSummary(transactions: List<Transaction>) {
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val netSavings = income - expenses

        // Update UI
        binding.tvTotalIncome.text = numberFormat.format(income)
        binding.tvTotalExpenses.text = numberFormat.format(expenses)
        binding.tvNetSavings.text = numberFormat.format(netSavings)

        // Update savings status message
        val statusMessage = when {
            netSavings > 0 -> "Great job saving!"
            netSavings == 0.0 -> "Breaking even"
            else -> "Consider reducing expenses"
        }
        binding.tvSavingsStatus.text = statusMessage
    }

    private fun updateRecentTransactions(transactions: List<Transaction>) {
        // Show most recent 5 transactions
        val recentTransactions = transactions.take(5)

        if (recentTransactions.isEmpty()) {
            binding.rvRecentTransactions.visibility = View.GONE
            binding.tvNoTransactions.visibility = View.VISIBLE
        } else {
            binding.rvRecentTransactions.visibility = View.VISIBLE
            binding.tvNoTransactions.visibility = View.GONE
            recentTransactionsAdapter.updateTransactions(recentTransactions)
        }
    }

    private fun setupUI() {
        // Display user name
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userName = currentUser?.email?.substringBefore('@') ?: "User"
        binding.tvUserName.text = "Hello, ${userName.replaceFirstChar { it.uppercase() }}!"

        // Initialize with default values
        showDefaultState()
    }

    private fun setupRecyclerView() {
        recentTransactionsAdapter = DashboardTransactionAdapter { transaction ->
            // Click to edit transaction
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)  // Fixed: lowercase to match EditTransactionActivity
            startActivity(intent)
        }

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = recentTransactionsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        binding.btnViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.cardIncome.setOnClickListener {
            val intent = Intent(this, IncomeActivity::class.java)
            startActivity(intent)
        }

        binding.cardExpenses.setOnClickListener {
            val intent = Intent(this, ExpenseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDefaultState() {
        // Show default values when no data or error
        binding.tvTotalIncome.text = "$0.00"
        binding.tvTotalExpenses.text = "$0.00"
        binding.tvNetSavings.text = "$0.00"
        binding.tvSavingsStatus.text = "Start tracking your finances!"

        binding.rvRecentTransactions.visibility = View.GONE
        binding.tvNoTransactions.visibility = View.VISIBLE
    }

    private fun logout() {
        // Clear user session
        preferenceHelper.clearUserSession()

        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Navigate to login
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}