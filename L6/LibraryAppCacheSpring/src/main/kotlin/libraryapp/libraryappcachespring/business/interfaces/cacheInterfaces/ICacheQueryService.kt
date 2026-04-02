package libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces

interface ICacheQueryService {
    fun getAllBooks(format: String): String?
    fun findBook(format: String = "json", author: String = "", title: String = "", publisher: String = ""): String?
}