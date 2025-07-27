package com.example.savehaven.data

import com.google.android.gms.maps.model.LatLng

data class Bank(
    val name: String,
    val address: String,
    val latLng: LatLng
)
