package com.sd.laborator.services

import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.Coordinates
import com.sd.laborator.pojo.WeatherForecastData
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import kotlin.math.roundToInt

@Service
class WeatherForecastService(private val timeService: TimeService) : WeatherForecastInterface {

    override fun getForecastData(locationId: Coordinates?): WeatherForecastData {

        require(locationId != null) { "Location coordinates cannot be null" }

        val forecastDataURL = URL(
            "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=${locationId.latitude}&longitude=${locationId.longitude}" +
                    "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m"
        )

        val rawResponse = forecastDataURL.readText()

        print(rawResponse)

        val root = JSONObject(rawResponse)
        val current = root.getJSONObject("current")

        return WeatherForecastData(
            location = locationId.location,
            date = timeService.getCurrentTime(),
            windDirection = current.getDouble("wind_direction_10m").roundToInt().toString(),
            windSpeed = current.getDouble("wind_speed_10m").roundToInt(),
            currentTemp = current.getDouble("temperature_2m").roundToInt(),
            humidity = current.getDouble("relative_humidity_2m").roundToInt()
        )
    }
}