package com.example.savehaven.ui

// Import required Android and Google Maps packages
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.savehaven.R
import com.example.savehaven.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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

    // View binding for layout
    private lateinit var binding: ActivityMapBinding

    // GoogleMap object
    private lateinit var map: GoogleMap

    // Navigation drawer
    private lateinit var drawerLayout: DrawerLayout

    // HTTP client for API calls
    private val client = OkHttpClient()

    // Keyword used to search for nearby places
    private var currentSearchKeyword = "bank"

    // Tracks the last location searched to avoid repeated searches on small moves
    private var lastSearchLatLng: LatLng? = null

    // Threshold to determine significant map movement
    private val minMovementThresholdMeters = 10000

    // Location permission request code
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Used to retrieve user's current location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar and navigation drawer
        setupToolbar()
        setupNavigationDrawer()

        // Initialize the Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // Initialize fused location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Load the map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup search button click listener
        setupClickListeners()
    }

    // Set up the top toolbar
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Find a Bank"
    }

    // Set up the navigation drawer and its click listeners
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
        navigationView.setCheckedItem(R.id.nav_find_bank)
    }

    // Handle navigation drawer item clicks
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            R.id.nav_add_transaction -> {
                startActivity(Intent(this, AddTransactionActivity::class.java))
            }
            R.id.nav_income_overview -> {
                startActivity(Intent(this, IncomeActivity::class.java))
                finish()
            }
            R.id.nav_expense_overview -> {
                startActivity(Intent(this, ExpenseActivity::class.java))
                finish()
            }
            R.id.nav_find_bank -> {
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

    // Handle back press to close the drawer if open
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Ensure the correct navigation item is highlighted when resuming
    override fun onResume() {
        super.onResume()
        binding.navView.setCheckedItem(R.id.nav_find_bank)
    }

    // Setup the search button to trigger geolocation lookup
    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            val location = binding.etSearch.text.toString()
            if (location.isNotEmpty()) {
                searchLocation(location)
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Map is ready to use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Set default map location to New York City
        val defaultLocation = LatLng(40.7128, -74.0060)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        // Detect camera movement and trigger search if necessary
        map.setOnCameraIdleListener {
            val newCenter = map.cameraPosition.target
            if (lastSearchLatLng == null || distanceBetween(lastSearchLatLng!!, newCenter) > minMovementThresholdMeters) {
                lastSearchLatLng = newCenter
                performNearbySearch(newCenter.latitude, newCenter.longitude, currentSearchKeyword)
            }
        }

        checkLocationPermissionAndEnable()
    }

    // Request location permission if not granted
    private fun checkLocationPermissionAndEnable() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            enableUserLocation()
        }
    }

    // Enable location tracking on the map
    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Handle the result of the location permission dialog
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Search for a location using geocoding
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
                performNearbySearch(latLng.latitude, latLng.longitude, currentSearchKeyword)
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error searching location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Search for nearby places using Google Places API
    private fun performNearbySearch(lat: Double, lng: Double, keyword: String) {
        val apiKey = getString(R.string.google_maps_key)
        val radius = 2000 // in meters
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=$lat,$lng&radius=$radius&keyword=$keyword&key=$apiKey"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "Failed to load nearby places", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonString = response.body?.string()
                    val jsonObject = JSONObject(jsonString ?: "")
                    val results = jsonObject.getJSONArray("results")

                    runOnUiThread {
                        map.clear()
                        for (i in 0 until results.length()) {
                            val place = results.getJSONObject(i)
                            val name = place.optString("name")
                            val geometry = place.getJSONObject("geometry").getJSONObject("location")
                            val placeLat = geometry.getDouble("lat")
                            val placeLng = geometry.getDouble("lng")
                            val position = LatLng(placeLat, placeLng)
                            map.addMarker(MarkerOptions().position(position).title(name))
                        }

                        if (results.length() == 0) {
                            Toast.makeText(this@MapActivity, "No places found nearby", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MapActivity, "Found ${results.length()} nearby", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MapActivity, "Error parsing nearby data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Calculate distance between two LatLng points
    private fun distanceBetween(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        return results[0]
    }
}
