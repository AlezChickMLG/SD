package com.sd.laborator.pojo

data class WeatherForecastData (
    val location: String?,
    var date: String,
    var windDirection: String, // km/h
    var windSpeed: Int,
    var currentTemp: Int,
    var humidity: Int // procent){}
)
