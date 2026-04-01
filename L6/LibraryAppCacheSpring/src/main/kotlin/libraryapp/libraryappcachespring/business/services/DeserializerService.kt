package libraryapp.libraryappcachespring.business.services

import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IDeserializerService
import libraryapp.libraryappcachespring.business.models.Book
import libraryapp.libraryappcachespring.business.models.Cache
import libraryapp.libraryappcachespring.business.models.Content
import org.springframework.stereotype.Service

@Service
class DeserializerService : IDeserializerService {
    override fun deserialize(databaseResult: Cache): List<Book> {
        if (databaseResult.query == "getAllBooks") {
            return getAllDeserialize(databaseResult)
        }

        return listOf()
    }

    private fun getAllDeserialize(databaseResult: Cache): List<Book> {
        val result = databaseResult.result.split("~~")
        return result.map {
            val book = it.split("~")
            Book(
                Content(
                    book.getOrNull(0)?.substring("author:".length),
                    book.getOrNull(1)?.substring("text:".length),
                    book.getOrNull(2)?.substring("name:".length),
                    book.getOrNull(3)?.substring("publisher:".length)
                )
            )
        }
    }
}