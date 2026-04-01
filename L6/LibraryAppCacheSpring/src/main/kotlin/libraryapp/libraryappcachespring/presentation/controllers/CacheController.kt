package libraryapp.libraryappcachespring.presentation.controllers

import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.ICacheService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IFormatterService
import libraryapp.libraryappcachespring.business.interfaces.ILibraryPrinterService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IAPIResultService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IDeserializerService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.ITimeService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IWebService
import libraryapp.libraryappcachespring.business.models.Book
import libraryapp.libraryappcachespring.business.models.Cache
import libraryapp.libraryappcachespring.business.models.Content
import libraryapp.libraryappcachespring.business.services.APIResultService
import libraryapp.libraryappcachespring.business.services.DeserializerService

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class CacheController (
    private val cacheService: ICacheService,
    private val libraryPrinterService: ILibraryPrinterService,
    private val webService: IWebService,
    private val formatterService: IFormatterService,
    private val apiResultService: IAPIResultService,
    private val deserializerService: IDeserializerService,
    private val timeService: ITimeService
) {
    @RequestMapping("/print", method = [RequestMethod.GET])
    @ResponseBody
    fun cachePrint(
        @RequestParam(required = true, value = "format", defaultValue = "html") format: String
    ): String? {
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