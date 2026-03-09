package com.sd.laborator.interfaces

import com.sd.laborator.pojo.Coordinates

interface LocationSearchInterface {
    fun getLocationId(locationName: String): Coordinates?
}