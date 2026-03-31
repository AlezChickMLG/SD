package libraryapp.libraryappspring.business.interfaces

import libraryapp.libraryappspring.business.models.Book

interface IRawPrinter {
    fun printRaw(books: Set<Book>): String
}