package libraryapp.libraryappcachespring.business.interfaces

import libraryapp.libraryappcachespring.business.models.Book

interface IHTMLPrinter {
    fun printHTML(books: Set<Book>): String
}
