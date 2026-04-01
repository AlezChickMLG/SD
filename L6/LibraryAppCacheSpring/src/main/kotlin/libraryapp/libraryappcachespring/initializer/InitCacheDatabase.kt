package libraryapp.libraryappcachespring.initializer

import libraryapp.libraryappcachespring.business.services.CacheService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class InitCacheDatabase (
    private val cacheService: CacheService
) : CommandLineRunner {
    override fun run(vararg args: String) {
        cacheService.createTable()
        println("Cache database created")
    }
}