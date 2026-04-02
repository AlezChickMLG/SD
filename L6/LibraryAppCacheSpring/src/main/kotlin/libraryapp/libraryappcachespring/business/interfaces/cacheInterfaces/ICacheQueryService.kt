package libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces

interface ICacheQueryService {
    fun getAllBooks(format: String): String?
}