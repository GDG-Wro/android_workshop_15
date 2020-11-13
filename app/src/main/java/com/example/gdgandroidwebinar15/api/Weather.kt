package com.example.gdgandroidwebinar15.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Weather(
    @Json(name = "consolidated_weather") val forecasts: List<Forecast>
)
