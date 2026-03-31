package libraryapp.libraryappspring.persistence.repository

import libraryapp.libraryappspring.business.models.Book
import libraryapp.libraryappspring.persistence.interfaces.IBookRepository
import libraryapp.libraryappspring.persistence.rowMapper.BookRowMapper
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class BookRepository (
    private val jdbcTemplate: JdbcTemplate,
    private val bookRowMapper: BookRowMapper
) : IBookRepository{

    override fun getBookByAuthor(author: String): List<Book?> {
        val sqlGet = "SELECT * FROM BOOK " +
                "WHERE author = ?"
        try {
            return jdbcTemplate.query(sqlGet, bookRowMapper, author)
        } catch (e: DataAccessException) {
            println("[getBookByAuthor] Error: $e")
        }

        return listOf()
    }

    override fun getBookByAuthorTitle(author: String, title: String): Book? {
        val sql = "SELECT * FROM BOOK " +
                "WHERE author = ? AND title = ?"
        try {
            return jdbcTemplate.queryForObject(sql, bookRowMapper, author, title)
        } catch (e: DataAccessException) {
            println("[getBookByAuthorTitle] Error: $e")
        }

        return null
    }

    override fun getBookByTitle(title: String): List<Book?> {
        val sqlGet = "SELECT * FROM BOOK " +
                "WHERE title = ?"
        try {
            return jdbcTemplate.query(sqlGet, bookRowMapper, title)
        } catch (e: DataAccessException) {
            println("[getBookByTitle] Error: $e")
        }

        return listOf()
    }

    override fun getBookByPublisher(publisher: String): List<Book?> {
        val sqlGet = "SELECT * FROM BOOK " +
                "WHERE publisher = ?"
        try {
            return jdbcTemplate.query(sqlGet, bookRowMapper, publisher)
        } catch (e: DataAccessException) {
            println("[getBookByPublisher] Error: $e")
        }

        return listOf()
    }

    override fun getAllBooks(): List<Book?> {
        val sql = "SELECT * FROM BOOK"

        try {
            return jdbcTemplate.query(sql, bookRowMapper)
        } catch (e: DataAccessException) {
            println("[getAllBooks] Error: $e")
        }

        return listOf()
    }

    override fun addBook(book: Book) {
        val sqlAdd: String = "INSERT INTO BOOK(author, title, publisher, text) VALUES (?, ?, ?, ?)"
        try {
            jdbcTemplate.update(sqlAdd, book.author, book.name, book.publisher, book.content)
        } catch (e: DataAccessException) {
            println("[addBook] Error: $e")
        }
    }

    override fun createTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS BOOK (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "author VARCHAR," +
                "title VARCHAR," +
                "publisher VARCHAR," +
                "text TEXT)")
    }

}