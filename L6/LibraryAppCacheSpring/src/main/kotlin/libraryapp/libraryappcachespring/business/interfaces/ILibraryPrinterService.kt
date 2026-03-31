package libraryapp.libraryappcachespring.business.interfaces

import libraryapp.libraryappcachespring.business.models.Book


interface ILibraryPrinterService {
    fun printHTML(books: Set<Book>): String
    fun printJSON(books: Set<Book>): String
    fun printRaw(books: Set<Book>): String
}