package com.example.savehaven.ui

import android.location.Geocoder
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.savehaven.R
import com.example.savehaven.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search Map"

        // ✅ Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // Load the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Handle search
        binding.btnSearch.setOnClickListener {
            val location = binding.etSearch.text.toString()
            if (location.isNotEmpty()) {
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
            }
        }

        // Back button in layout
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                val jsonString = response.body?.string()
                val jsonObject = JSONObject(jsonString ?: "")
                val results = jsonObject.getJSONArray("results")

                runOnUiThread {
                    for (i in 0 until results.length()) {
                        val bank = results.getJSONObject(i)
                        val name = bank.optString("name")
                        val geometry = bank.getJSONObject("geometry").getJSONObject("location")
                        val lat = geometry.getDouble("lat")
                        val lng = geometry.getDouble("lng")
                        val position = LatLng(lat, lng)
                        map.addMarker(MarkerOptions().position(position).title(name))
                    }
                }
            }
        })
    }
}
