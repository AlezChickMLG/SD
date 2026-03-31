package com.sd.laborator.interfaces

import com.sd.laborator.pojo.Coordinates
import com.sd.laborator.pojo.WeatherForecastData

interface WeatherForecastInterface {
    fun getForecastData(locationId: Coordinates?): WeatherForecastData
}