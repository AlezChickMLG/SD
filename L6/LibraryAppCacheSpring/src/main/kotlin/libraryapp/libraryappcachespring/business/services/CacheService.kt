package libraryapp.libraryappcachespring.business.services

import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.ICacheService
import libraryapp.libraryappcachespring.business.models.Cache
import libraryapp.libraryappcachespring.persistence.repository.CacheRepository
import org.springframework.stereotype.Service

@Service
class CacheService (
    private val cacheRepository: CacheRepository
) : ICacheService {
    override fun createTable() {
        cacheRepository.createTable()
    }

    override fun deleteCache(cache: Cache) {
        cacheRepository.deleteCache(cache)
    }

    override fun updateCache(cache: Cache) {
        cacheRepository.updateCache(cache)
    }

    override fun addCache(cache: Cache) {
        cacheRepository.addCache(cache)
    }

    override fun getCacheByQuery(query: String): Cache? {
        return cacheRepository.getCacheByQuery(query)
    }

    override fun getAllCaches(): List<Cache>? {
        return cacheRepository.getAllCaches()
    }
}