package com.example.gdgandroidwebinar15.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDate

@Dao
abstract class WeatherDao {
    @Query("SELECT * from forecast ORDER BY date ASC")
    abstract fun getForecasts(): Flow<List<ForecastEntity>>

    @Query("SELECT COUNT(*) from forecast")
    abstract suspend fun getForecastCount(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(forecasts: List<ForecastEntity>)

    @Query("DELETE FROM forecast WHERE date < :date")
    abstract suspend fun deleteOlderThan(date: LocalDate)

    @Transaction
    open suspend fun replaceForecast(forecasts: List<ForecastEntity>) {
        insert(forecasts)
        val forecastDate = forecasts.minByOrNull { it.date }?.date
        if (forecastDate != null) deleteOlderThan(forecastDate)
    }
}
