package libraryapp.libraryappspring.business.interfaces

import libraryapp.libraryappspring.business.models.Book

interface IJSONPrinter {
    fun printJSON(books: Set<Book>): String
}