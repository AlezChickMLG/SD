package libraryapp.libraryappspring.business.interfaces

import libraryapp.libraryappspring.business.models.Book

interface IHTMLPrinter {
    fun printHTML(books: Set<Book>): String
}
