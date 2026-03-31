package libraryapp.libraryappcachespring.business.interfaces

import libraryapp.libraryappcachespring.business.models.Book

interface IJSONPrinter {
    fun printJSON(books: Set<Book>): String
}