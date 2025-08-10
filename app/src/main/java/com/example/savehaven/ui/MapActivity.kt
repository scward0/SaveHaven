package com.example.savehaven.ui

import android.Manifest
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
import com.example.savehaven.utils.NavigationHandler
import com.example.savehaven.utils.setNavigationSelection
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

/**
 * Bank finder using Google Maps and Places API
 * Users can search by location or use current GPS location to find nearby banks
 */
class MapActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private lateinit var drawerLayout: DrawerLayout

    // For Places API calls
    private val client = OkHttpClient()
    private var currentSearchKeyword = "bank"

    // Prevent too many API calls when map moves
    private var lastSearchLatLng: LatLng? = null
    private val minMovementThresholdMeters = 10000 // Only search again if moved 10km

    // Location permissions
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigationDrawer()

        // Initialize Google Places if not already done
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // Set up location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Load the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up search functionality
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Find a Bank"
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

    // Maps can be used alongside other features, so finish when going to main navigation
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

    override fun onResume() {
        super.onResume()
        setNavigationSelection(this, binding.navView)
    }

    // Set up the search button
    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            val location = binding.etSearch.text.toString()
            if (location.isNotEmpty()) {
                searchLocation(location) // Search by entered location
            } else {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Called when Google Map is ready to use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Start map centered on NYC as default
        val defaultLocation = LatLng(40.7128, -74.0060)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        // When user moves the map, search for banks in new area
        map.setOnCameraIdleListener {
            val newCenter = map.cameraPosition.target
            // Only search if they moved far enough (saves API calls)
            if (lastSearchLatLng == null || distanceBetween(lastSearchLatLng!!, newCenter) > minMovementThresholdMeters) {
                lastSearchLatLng = newCenter
                performNearbySearch(newCenter.latitude, newCenter.longitude, currentSearchKeyword)
            }
        }

        // Try to enable user location if we have permission
        checkLocationPermissionAndEnable()
    }

    // Request location permission if we don't have it
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

    // Enable the blue dot showing user's current location
    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            // Move camera to user's location
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

    // Handle the result of permission request
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

    // Search for a location by name/address (like "New York" or zip code)
    private fun searchLocation(location: String) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addressList = geocoder.getFromLocationName(location, 1)

            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)

                // Clear previous markers and add new one for searched location
                map.clear()
                map.addMarker(MarkerOptions().position(latLng).title(location))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))

                // Search for banks near this location
                performNearbySearch(latLng.latitude, latLng.longitude, currentSearchKeyword)
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error searching location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Use Google Places API to find nearby banks
    private fun performNearbySearch(lat: Double, lng: Double, keyword: String) {
        val apiKey = getString(R.string.google_maps_key)
        val radius = 2000 // Search within 2km
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=$lat,$lng&radius=$radius&keyword=$keyword&key=$apiKey"

        val request = Request.Builder().url(url).build()

        // Make API call in background
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
                        // Clear old markers
                        map.clear()

                        // Add marker for each bank found
                        for (i in 0 until results.length()) {
                            val place = results.getJSONObject(i)
                            val name = place.optString("name")
                            val geometry = place.getJSONObject("geometry").getJSONObject("location")
                            val placeLat = geometry.getDouble("lat")
                            val placeLng = geometry.getDouble("lng")
                            val position = LatLng(placeLat, placeLng)

                            map.addMarker(MarkerOptions().position(position).title(name))
                        }

                        // Give user feedback
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

    // Calculate distance between two map points to avoid too many API calls
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