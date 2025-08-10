package com.example.savehaven.ui

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.savehaven.R
import com.example.savehaven.data.*
import com.example.savehaven.databinding.ActivityDashboardBinding
import com.example.savehaven.utils.NavigationHandler
import com.example.savehaven.utils.PreferenceHelper
import com.example.savehaven.utils.setNavigationSelection
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.*

/**
 * Main dashboard - the heart of the SaveHaven app
 * Shows financial summary, recent transactions, and provides quick access to key features
 */
@Suppress("DEPRECATION")
class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var recentTransactionsAdapter: DashboardTransactionAdapter
    private lateinit var drawerLayout: DrawerLayout

    // Format money consistently across the dashboard
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up our data access and preferences
        transactionRepository = TransactionRepository()
        preferenceHelper = PreferenceHelper(this)

        setupToolbar()
        setupNavigationDrawer()
        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // Load user's financial data
        loadData()

        // Handle notification permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Set up notifications for educational tips
        createNotificationChannel()

        // Show educational tip if user has them enabled and we got one from MainActivity
        val showTips = PreferenceHelper(this).getBoolean("education_facts", true)
        val tip = intent.getStringExtra("financial_tip")
        if (tip != null && showTips) {
            showFinancialTipDialog(tip)
            showFinancialTipNotification(tip)
        }
    }

    // Show educational tip as a popup dialog
    private fun showFinancialTipDialog(tip: String) {
        AlertDialog.Builder(this)
            .setTitle("💡 Financial Tip")
            .setMessage(tip)
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    // Create notification channel for Android 8.0+
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "financial_tips_channel",
                "Financial Tips",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily financial education tips"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // Show educational tip as a notification too
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showFinancialTipNotification(tip: String) {
        val builder = NotificationCompat.Builder(this, "financial_tips_channel")
            .setSmallIcon(R.drawable.educational)
            .setContentTitle("💡 Financial Tip")
            .setContentText(tip)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = NotificationManagerCompat.from(this)
        manager.notify(1002, builder.build())
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "SaveHaven Dashboard"
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

    // Don't finish when navigating since this IS the main screen
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return NavigationHandler.handleNavigation(this, item, drawerLayout, shouldFinishOnMainNavigation = false)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Refresh data when returning from other screens
    override fun onResume() {
        super.onResume()
        loadData()
        setNavigationSelection(this, binding.navView)
    }

    // Check if user is still logged in, redirect if not
    private fun loadData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        loadTransactions()
    }

    // Load all user transactions and update the dashboard
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

    // Update both financial summary and recent transactions
    private fun updateUI(transactions: List<Transaction>) {
        updateFinancialSummary(transactions)
        updateRecentTransactions(transactions)
    }

    // Calculate and display income, expenses, and net savings
    private fun updateFinancialSummary(transactions: List<Transaction>) {
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val netSavings = income - expenses

        // Update the summary cards
        binding.tvTotalIncome.text = numberFormat.format(income)
        binding.tvTotalExpenses.text = numberFormat.format(expenses)
        binding.tvNetSavings.text = numberFormat.format(netSavings)

        // Give user motivational feedback based on their savings
        val statusMessage = when {
            netSavings > 0 -> "Great job saving!"
            netSavings == 0.0 -> "Breaking even"
            else -> "Consider reducing expenses"
        }
        binding.tvSavingsStatus.text = statusMessage
    }

    // Show the 5 most recent transactions
    private fun updateRecentTransactions(transactions: List<Transaction>) {
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

    // Set up initial UI state and user greeting
    private fun setupUI() {
        // Show personalized greeting using email username
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userName = currentUser?.email?.substringBefore('@') ?: "User"
        binding.tvUserName.text = "Hello, ${userName.replaceFirstChar { it.uppercase() }}!"

        // Show default state until data loads
        showDefaultState()
    }

    // Set up recycler view for recent transactions
    private fun setupRecyclerView() {
        recentTransactionsAdapter = DashboardTransactionAdapter { transaction ->
            // When user taps a transaction, open edit screen
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transaction_id", transaction.id)
            startActivity(intent)
        }

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = recentTransactionsAdapter
        }
    }

    // Wire up all the dashboard buttons
    private fun setupClickListeners() {
        // Quick add transaction
        binding.btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        // View all transactions
        binding.btnViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        // Income analysis
        binding.cardIncome.setOnClickListener {
            startActivity(Intent(this, IncomeActivity::class.java))
        }

        // Expense analysis
        binding.cardExpenses.setOnClickListener {
            startActivity(Intent(this, ExpenseActivity::class.java))
        }
    }

    // Show default values when no data is available
    private fun showDefaultState() {
        binding.tvTotalIncome.text = "$0.00"
        binding.tvTotalExpenses.text = "$0.00"
        binding.tvNetSavings.text = "$0.00"
        binding.tvSavingsStatus.text = "Start tracking your finances!"

        binding.rvRecentTransactions.visibility = View.GONE
        binding.tvNoTransactions.visibility = View.VISIBLE
    }

    // Redirect to login if user session is invalid
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}