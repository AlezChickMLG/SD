package libraryapp.libraryappcachespring.persistence.interfaces

import libraryapp.libraryappcachespring.business.models.Cache

interface ICacheRepository {
    fun createTable()
    fun addCache(cache: Cache)
    fun deleteCache(cache: Cache)
    fun updateCache(cache: Cache)
    fun getCacheByQuery(query: String): Cache?
    fun getAllCaches(): List<Cache>?
}