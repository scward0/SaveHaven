package com.example.savehaven.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.savehaven.R
import com.example.savehaven.data.Bank
import com.example.savehaven.ui.BankAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class BankFinderActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BankAdapter
    private var bankList: MutableList<Bank> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_finder)

        recyclerView = findViewById(R.id.bankRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BankAdapter(bankList) { bank ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(bank.latLng, 15f))
        }
        recyclerView.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        } else {
            setupMap()
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                loadNearbyBanks(userLatLng)
            }
        }
    }

    private fun loadNearbyBanks(userLatLng: LatLng) {
        val banks = listOf(
            Bank("Chase Bank", "123 Main St", LatLng(userLatLng.latitude + 0.001, userLatLng.longitude + 0.001)),
            Bank("Bank of America", "456 Elm St", LatLng(userLatLng.latitude - 0.001, userLatLng.longitude - 0.001))
        )
        bankList.clear()
        bankList.addAll(banks)
        adapter.notifyDataSetChanged()

        banks.forEach { bank ->
            map.addMarker(MarkerOptions().position(bank.latLng).title(bank.name))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap()
        } else {
            Toast.makeText(this, "Location permission is required to use this feature.", Toast.LENGTH_LONG).show()
        }
    }
}
