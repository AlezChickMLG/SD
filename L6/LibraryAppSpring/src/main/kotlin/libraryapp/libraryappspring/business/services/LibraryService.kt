package libraryapp.libraryappspring.business.services

import libraryapp.libraryappspring.business.interfaces.ILibraryService
import libraryapp.libraryappspring.business.models.Book
import libraryapp.libraryappspring.persistence.repository.BookRepository
import org.springframework.stereotype.Service

@Service
class LibraryService (
    private val bookRepository: BookRepository
) : ILibraryService {
    override fun createTable() {
        bookRepository.createTable()
    }

    override fun addBook(book: Book) {
        bookRepository.addBook(book)
    }

    override fun getAllBooks(): List<Book?> {
        return bookRepository.getAllBooks()
    }

    override fun getBookByAuthor(author: String): List<Book?> {
        return bookRepository.getBookByAuthor(author)
    }

    override fun getBookByAuthorTitle(
        author: String,
        title: String
    ): Book? {
        return bookRepository.getBookByAuthorTitle(author, title)
    }

    override fun getBookByTitle(title: String): List<Book?> {
        return bookRepository.getBookByTitle(title)
    }

    override fun getBookByPublisher(publisher: String): List<Book?> {
        return bookRepository.getBookByPublisher(publisher)
    }
}