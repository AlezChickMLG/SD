package libraryapp.libraryappspring.presentation.controllers

import libraryapp.libraryappspring.business.interfaces.ILibraryPrinterService
import libraryapp.libraryappspring.business.models.Book
import libraryapp.libraryappspring.business.services.LibraryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class LibraryPrinterController {
    @Autowired
    private lateinit var _libraryPrinterService: ILibraryPrinterService

    @Autowired
    private lateinit var _libraryService: LibraryService

    @RequestMapping("/print", method = [RequestMethod.GET])
    @ResponseBody
    fun customPrint(@RequestParam(required = true, name = "format",
        defaultValue = "") format: String): String {
        return when (format) {
            "html" ->
                _libraryPrinterService.printHTML(_libraryService.getAllBooks().filterNotNull().toSet())
            "json" ->
                _libraryPrinterService.printJSON(_libraryService.getAllBooks().filterNotNull().toSet())
            "raw" ->
                _libraryPrinterService.printRaw(_libraryService.getAllBooks().filterNotNull().toSet())
            else -> "Not implemented"
        }
    }

    @RequestMapping("/find", method = [RequestMethod.GET])
    @ResponseBody
    fun customFind(
        @RequestParam(required = false, name = "author", defaultValue = "") author: String,
        @RequestParam(required = false, name = "title", defaultValue = "") title: String,
        @RequestParam(required = false, name = "publisher", defaultValue = "") publisher: String
    ): String {
        if (author != "")
            return this._libraryPrinterService.printJSON(this._libraryService.getBookByAuthor(author).filterNotNull().toSet())
        if (title != "")
            return this._libraryPrinterService.printJSON(this._libraryService.getBookByTitle(title).filterNotNull().toSet())
        if (publisher != "")
            return this._libraryPrinterService.printJSON(this._libraryService.getBookByPublisher(publisher).filterNotNull().toSet())
        return "Not a valid field"
    }

    @RequestMapping("find-and-print", method = [RequestMethod.GET])
    @ResponseBody
    fun customFindAndPrint(
        @RequestParam(required = false, name = "author", defaultValue = "") author: String,
        @RequestParam(required = false, name = "format", defaultValue = "html") format: String
    ): String {
        if (author != "") {
            val book = _libraryService.getBookByAuthor(author)
            return when (format) {
                "html" -> _libraryPrinterService.printHTML(book.filterNotNull().toSet())
                "json" -> _libraryPrinterService.printJSON(book.filterNotNull().toSet())
                "raw" -> _libraryPrinterService.printRaw(book.filterNotNull().toSet())
                else -> "Not implemented"
            }
        }

        return "No author"
    }
}