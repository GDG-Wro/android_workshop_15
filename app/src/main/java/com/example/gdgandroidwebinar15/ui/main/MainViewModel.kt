package com.example.gdgandroidwebinar15.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gdgandroidwebinar15.Consumable
import com.example.gdgandroidwebinar15.domain.WeatherRepository
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.threeten.bp.format.DateTimeFormatter

class MainViewModel(private val weatherRepository: WeatherRepository) : ViewModel() {
    private val _models = MutableStateFlow(MainUiModel())
    val models: StateFlow<MainUiModel> = _models

    init {
        viewModelScope.launch {
            weatherRepository.getForecast()
                .onStart { if (!weatherRepository.hasForecast()) fetchForecast() }
                .collect { _models.value = models.value.copy(forecasts = it) }
        }
    }

    fun fetchForecast() = viewModelScope.launch {
        Firebase.analytics.logEvent("fetch_forecast") {
            val lastForecastDate = _models.value.forecasts.firstOrNull()
            if (lastForecastDate != null) {
                param(
                    "previous_forecast_date",
                    lastForecastDate.date.format(DateTimeFormatter.ISO_DATE)
                )
            }
        }
        _models.value = models.value.copy(isLoading = true)
        val isSuccessful = weatherRepository.fetchForecast()
        val error = Consumable(if (isSuccessful) null else Unit)
        _models.value = models.value.copy(isLoading = false, error = error)
    }
}
