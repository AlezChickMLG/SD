package libraryapp.libraryappcachespring.business.interfaces

import libraryapp.libraryappcachespring.business.models.Book

interface IRawPrinter {
    fun printRaw(books: Set<Book>): String
}