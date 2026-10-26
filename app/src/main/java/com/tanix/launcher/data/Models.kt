package com.tanix.launcher.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap?, val adaptive: Boolean, val banner: ImageBitmap? = null)

data class Weather(val temp: Double, val feels: Double, val code: Int, val wind: Double)

data class City(val name: String, val region: String, val lat: Double, val lon: Double)
