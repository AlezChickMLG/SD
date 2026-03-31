package beerApp.persistence.interfaces

import beerApp.models.Beer

interface IBeerRepository {
    // Create
    fun createTable()
    fun add(beer: Beer)
    // Retrieve
    fun getAll(): MutableList<Beer?>
    fun getByName(name: String): Beer?
    fun getByPrice(price: Float): MutableList<Beer?>
    // Update
    fun update(beer: Beer)
    // Delete
    fun delete(name: String)
}