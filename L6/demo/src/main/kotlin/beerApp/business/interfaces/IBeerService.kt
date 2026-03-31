package beerApp.business.interfaces

import beerApp.models.Beer

interface IBeerService {
    fun createBeerTable()
    fun addBeer(beer: Beer)
    fun getBeers(): String
    fun getBeerByName(name: String): String?
    fun getBeerByPrice(price: Float): String
    fun updateBeer(beer: Beer)
    fun deleteBeer(name: String)
}