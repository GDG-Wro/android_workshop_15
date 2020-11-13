package com.example.gdgandroidwebinar15

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class WeatherApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}
