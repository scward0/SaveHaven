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
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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
import com.example.savehaven.utils.FinancialTipsProvider
import com.example.savehaven.utils.PreferenceHelper
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

@Suppress("DEPRECATION")
class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var recentTransactionsAdapter: DashboardTransactionAdapter
    private lateinit var drawerLayout: DrawerLayout

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize dependencies
        transactionRepository = TransactionRepository()
        preferenceHelper = PreferenceHelper(this)

        setupToolbar()
        setupNavigationDrawer()
        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // Load data
        loadData()

        // Request notification permission (Android 13+)
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

        // Create notification channel
        createNotificationChannel()

        // Show tip if user enabled it in Preferences
        val showTips = PreferenceHelper(this).getBoolean("education_facts", true)
        val tip = intent.getStringExtra("financial_tip")
        if (tip != null && showTips) {
            showFinancialTipDialog(tip)
            showFinancialTipNotification(tip)
        }

    }

    // Show financial tip pop-up
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

    // Create notification channel
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

    // Show financial tip notification
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

        // Set Dashboard as selected by default
        navigationView.setCheckedItem(R.id.nav_dashboard)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Already on dashboard, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_add_transaction -> {
                startActivity(Intent(this, AddTransactionActivity::class.java))
            }
            R.id.nav_income_overview -> {
                startActivity(Intent(this, IncomeActivity::class.java))
            }
            R.id.nav_expense_overview -> {
                startActivity(Intent(this, ExpenseActivity::class.java))
            }
            R.id.nav_transaction_history -> {
                startActivity(Intent(this, TransactionHistoryActivity::class.java))
            }
            R.id.nav_find_bank -> {
                startActivity(Intent(this, MapActivity::class.java))
            }
            R.id.nav_preferences -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
            }
            R.id.nav_logout -> {
                logout()
                return true
            }

        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
        // Reset navigation selection to dashboard when returning
        binding.navView.setCheckedItem(R.id.nav_dashboard)
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
            intent.putExtra("transaction_id", transaction.id)
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
            startActivity(Intent(this, IncomeActivity::class.java))
        }

        binding.cardExpenses.setOnClickListener {
            startActivity(Intent(this, ExpenseActivity::class.java))
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