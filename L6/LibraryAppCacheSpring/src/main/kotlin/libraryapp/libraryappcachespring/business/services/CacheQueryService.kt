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
                    when(format) {
                        "html" -> apiResultService.fromHTMLUpdate(result, "getAllBooks")
                        "json" -> apiResultService.fromJSONUpdate(result, "getAllBooks")
                        else -> "Not implemented"
                    }
                    println("Cache updated")
                }
                return result
            }
        }

        else {
            val result = webService.getPrint(format)
            if (result != null) {
                println("No cache | Non-nullable result")
                when(format) {
                    "html" -> apiResultService.fromHTMLAdd(result, "getAllBooks")
                    "json" -> apiResultService.fromJSONAdd(result, "getAllBooks")
                    else -> "Not implemented"
                }
                println("Cache added")
            }
            return result
        }
    }

    override fun findBook(
        format: String,
        author: String,
        title: String,
        publisher: String
    ): String? {
        var query = "findBook"

        query += if (author != "")
            "~author=$author"
        else if (title != "")
            "~title=$title"
        else if (publisher != "")
            "~publisher=$publisher"
        else {
            println("Error: No criteria")
            return null
        }

        val cache = cacheService.getCacheByQuery(query)

        if (cache != null) {
            if (timeService.getTime() + cache.timestamp < 100000) {
                val books = deserializerService.deserialize(cache)
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
                val result = webService.getFind(query.substring(query.indexOf("~") + 1, query.indexOf("=")), query.substring(query.indexOf("=") + 1))

                val filterName = query.substring(query.indexOf("~") + 1, query.indexOf("="))
                val filterValue = query.substring(query.indexOf("=") + 1)

                println("filterName: $filterName\nfilterValue: $filterValue")

                if (result != null) {
                    println("Cache too old")
                    when(format) {
                        "html" -> apiResultService.fromHTMLUpdate(result, query)
                        "json" -> apiResultService.fromJSONUpdate(result, query)
                        else -> "Not implemented"
                    }
                    println("Cache updated")
                }
                return result
            }
        }

        else {
            val result = webService.getFind(query.substring(query.indexOf("~") + 1, query.indexOf("=")), query.substring(query.indexOf("=") + 1))

            val filterName = query.substring(query.indexOf("~") + 1, query.indexOf("="))
            val filterValue = query.substring(query.indexOf("=") + 1)

            println("filterName: $filterName\nfilterValue: $filterValue")

            if (result != null) {
                println("No cache | Non-nullable result")
                when(format) {
                    "html" -> apiResultService.fromHTMLAdd(result, query)
                    "json" -> apiResultService.fromJSONAdd(result, query)
                    else -> "Not implemented"
                }
                println("Cache added")
            }
            return result
        }
    }
}