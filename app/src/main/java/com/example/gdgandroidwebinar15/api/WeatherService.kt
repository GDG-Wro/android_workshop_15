package com.example.gdgandroidwebinar15.api

import retrofit2.http.GET

interface WeatherService {
    @GET("api/location/44418/")
    suspend fun getWeather(): Weather
}
