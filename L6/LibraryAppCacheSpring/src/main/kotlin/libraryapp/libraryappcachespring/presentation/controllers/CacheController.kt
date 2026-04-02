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
import libraryapp.libraryappcachespring.business.services.CacheQueryService
import libraryapp.libraryappcachespring.business.services.DeserializerService

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class CacheController (
    private val cacheQueryService: CacheQueryService
) {
    @RequestMapping("/print", method = [RequestMethod.GET])
    @ResponseBody
    fun cachePrint(
        @RequestParam(required = true, value = "format", defaultValue = "html") format: String
    ): String? {
        return cacheQueryService.getAllBooks(format)
    }
}