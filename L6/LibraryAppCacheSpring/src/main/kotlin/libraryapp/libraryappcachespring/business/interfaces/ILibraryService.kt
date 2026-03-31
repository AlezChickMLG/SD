package libraryapp.libraryappcachespring.business.interfaces

import libraryapp.libraryappcachespring.business.models.Book

interface ILibraryService {
    fun createTable()
    fun addBook(book: Book)
    fun getBookByAuthor(author: String): List<Book?>
    fun getBookByAuthorTitle(author: String, title: String): Book?
    fun getBookByTitle(title: String): List<Book?>
    fun getBookByPublisher(publisher: String): List<Book?>
    fun getAllBooks(): List<Book?>
}