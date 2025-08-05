package com.example.savehaven.ui

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.savehaven.R
import com.example.savehaven.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.material.navigation.NavigationView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private lateinit var drawerLayout: DrawerLayout
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigationDrawer()

        // ✅ Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // Load the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Find a Bank"
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

        // Set Find a Bank as selected
        navigationView.setCheckedItem(R.id.nav_find_bank)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish() // Close this activity when navigating to main screens
            }
            R.id.nav_add_transaction -> {
                startActivity(Intent(this, AddTransactionActivity::class.java))
            }
            R.id.nav_income_overview -> {
                startActivity(Intent(this, IncomeActivity::class.java))
                finish() // Close this activity when navigating to main screens
            }
            R.id.nav_expense_overview -> {
                startActivity(Intent(this, ExpenseActivity::class.java))
                finish() // Close this activity when navigating to main screens
            }
            R.id.nav_find_bank -> {
                // Already on this screen, just close drawer
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_preferences -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
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
        // Reset navigation selection to find bank when returning
        binding.navView.setCheckedItem(R.id.nav_find_bank)
    }

    private fun setupClickListeners() {
        // Handle search
        binding.btnSearch.setOnClickListener {
            val location = binding.etSearch.text.toString()
            if (location.isNotEmpty()) {
                searchLocation(location)
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Set default location (you can customize this)
        val defaultLocation = LatLng(40.7128, -74.0060) // New York City
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
    }

    private fun searchLocation(location: String) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addressList = geocoder.getFromLocationName(location, 1)
            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)
                map.clear()
                map.addMarker(MarkerOptions().position(latLng).title(location))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                searchNearbyBanks(latLng.latitude, latLng.longitude)
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error searching location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchNearbyBanks(lat: Double, lng: Double) {
        val apiKey = getString(R.string.google_maps_key)
        val radius = 2000
        val url =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$lat,$lng&radius=$radius&type=bank&key=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "Failed to load banks", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonString = response.body?.string()
                    val jsonObject = JSONObject(jsonString ?: "")
                    val results = jsonObject.getJSONArray("results")

                    runOnUiThread {
                        for (i in 0 until results.length()) {
                            val bank = results.getJSONObject(i)
                            val name = bank.optString("name")
                            val geometry = bank.getJSONObject("geometry").getJSONObject("location")
                            val bankLat = geometry.getDouble("lat")
                            val bankLng = geometry.getDouble("lng")
                            val position = LatLng(bankLat, bankLng)
                            map.addMarker(MarkerOptions().position(position).title(name))
                        }

                        if (results.length() == 0) {
                            Toast.makeText(this@MapActivity, "No banks found nearby", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MapActivity, "Found ${results.length()} banks nearby", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MapActivity, "Error processing bank data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}