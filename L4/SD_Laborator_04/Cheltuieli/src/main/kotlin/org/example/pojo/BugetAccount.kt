package org.example.org.example.pojo

import org.example.pojo.Account

data class BugetAccount (
    val username: String,
    val bugetTotal: Double,
    var intretinere: Double = 0.0,
    var mancare: Double = 0.0,
    var distractie: Double = 0.0,
    var scoala: Double = 0.0,
    var personale: Double = 0.0
)