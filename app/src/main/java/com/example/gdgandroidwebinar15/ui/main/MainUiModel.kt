package com.example.gdgandroidwebinar15.ui.main

import com.example.gdgandroidwebinar15.Consumable
import com.example.gdgandroidwebinar15.domain.Forecast

data class MainUiModel(
    val forecasts: List<Forecast> = emptyList(),
    val isLoading: Boolean = false,
    val error: Consumable<Unit> = Consumable(null)
)
