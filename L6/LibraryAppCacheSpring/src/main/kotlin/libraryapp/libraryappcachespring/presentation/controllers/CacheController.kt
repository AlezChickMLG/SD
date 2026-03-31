package libraryapp.libraryappcachespring.presentation.controllers

import libraryapp.libraryappcachespring.business.services.CacheService
import libraryapp.libraryappcachespring.business.services.LibraryPrinterService

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class CacheController (
    private val cacheService: CacheService,
    private val libraryPrinterService: LibraryPrinterService
) {
    @RequestMapping("/print, ")
    fun cachePrint() {

    }
}