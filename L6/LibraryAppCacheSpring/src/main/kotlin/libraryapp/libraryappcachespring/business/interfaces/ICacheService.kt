package libraryapp.libraryappcachespring.business.interfaces

import libraryapp.libraryappcachespring.business.models.Cache

interface ICacheService {
    fun createTable()
    fun deleteCache(cache: Cache)
    fun updateCache(cache: Cache)
    fun addCache(cache: Cache)
    fun getCacheByQuery(query: String): Cache?
    fun getAllCaches(): List<Cache>?
}