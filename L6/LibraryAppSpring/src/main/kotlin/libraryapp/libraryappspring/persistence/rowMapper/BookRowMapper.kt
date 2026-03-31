package libraryapp.libraryappspring.persistence.rowMapper

import libraryapp.libraryappspring.business.models.Book
import libraryapp.libraryappspring.business.models.Content
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class BookRowMapper : RowMapper<Book> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int
    ): Book {
        return Book(
            Content(
                rs.getString("author"),
                rs.getString("text"),
                rs.getString("title"),
                rs.getString("publisher")
            )
        )
    }

}