package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.pojo.Coordinates
import org.springframework.stereotype.Service
import java.net.URL
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class LocationSearchService : LocationSearchInterface {
    override fun getLocationId(locationName: String): Coordinates? {
        val encodedLocationName = URLEncoder.encode(locationName, StandardCharsets.UTF_8.toString())

        // construire obiect de tip URL
        val locationSearchURL = URL("https://geocoding-api.open-meteo.com/v1/search?name=${encodedLocationName}&count=2&language=en&format=json")

        val rawResponse: String = locationSearchURL.readText()

        val responseRootObject = JSONObject(rawResponse)
        val responseContentObject = responseRootObject.getJSONArray("results").takeUnless { it.isEmpty }
            ?.getJSONObject(0)
        val latitude = responseContentObject?.getDouble("latitude")
        val longitude = responseContentObject?.getDouble("longitude")

        if (latitude != null && longitude != null) {
            return Coordinates(latitude, longitude, locationName)
        }

        return null
    }
}