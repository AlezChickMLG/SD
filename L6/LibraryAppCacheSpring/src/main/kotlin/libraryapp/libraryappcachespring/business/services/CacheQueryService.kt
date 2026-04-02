package libraryapp.libraryappcachespring.business.services

import libraryapp.libraryappcachespring.business.interfaces.ILibraryPrinterService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IAPIResultService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.ICacheQueryService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.ICacheService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IDeserializerService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IFormatterService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.ITimeService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IWebService
import org.springframework.stereotype.Service

@Service
class CacheQueryService (
    private val cacheService: ICacheService,
    private val libraryPrinterService: ILibraryPrinterService,
    private val webService: IWebService,
    private val formatterService: IFormatterService,
    private val apiResultService: IAPIResultService,
    private val deserializerService: IDeserializerService,
    private val timeService: ITimeService
) : ICacheQueryService {

    override fun getAllBooks(format: String): String? {
        val printCache = cacheService.getCacheByQuery("getAllBooks")

        if (printCache != null) {
            if (timeService.getTime() + 100_000 - printCache.timestamp < 100000) {
                val books = deserializerService.deserialize(printCache)
                println("Books deserialized: ")
                for (book in books) {
                    println(book)
                }

                return when (format) {
                    "html" ->
                        libraryPrinterService.printHTML(books.toSet())

                    "json" ->
                        libraryPrinterService.printJSON(books.toSet())

                    "raw" ->
                        libraryPrinterService.printRaw(books.toSet())

                    else -> "Not implemented"
                }
            }

            else {
                val result = webService.getPrint(format)
                if (result != null) {
                    println("Cache too old")
                    apiResultService.fromHTMLUpdate(result)
                    println("Cache updated")
                }
                return result
            }
        }

        else {
            val result = webService.getPrint(format)
            if (result != null) {
                println("No cache | Non-nullable result")
                apiResultService.fromHTMLAdd(result)
                println("Cache added")
            }
            return result
        }
    }
}