package libraryapp.libraryappcachespring.business.services

import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IFormatterService
import libraryapp.libraryappcachespring.business.models.Book
import libraryapp.libraryappcachespring.business.models.Content
import org.springframework.stereotype.Service

@Service
class FormatterService : IFormatterService {
    override fun fromHtml(html: String): List<Book> {
        val books = html.substring(html.indexOf("<body>") + "<body>".length, html.indexOf("</body>"))
            .split("<p>")
            .filter {
                it.isNotEmpty()
            }
            .map {
                it.substring(0, it.indexOf("</p>"))
            }

        val bookList = books.map {
            Book(
                Content(
                    it.substring(it.indexOf("<h4>") + "<h4>".length, it.indexOf("</h4>")),
                    it.substring(it.indexOf("</h5>") + "</h5>".length),
                    it.substring(it.indexOf("<h3>") + "<h3>".length, it.indexOf("</h3>")),
                    it.substring(it.indexOf("<h5>") + "<h5>".length, it.indexOf("</h5>"))
                )
            )
        }



        return bookList
    }

    override fun fromJSON(json: String): List<Book> {
        TODO("Not yet implemented")
    }

    override fun fromRaw(raw: String): List<Book> {
        TODO("Not yet implemented")
    }
}