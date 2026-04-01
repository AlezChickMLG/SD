package libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces

import libraryapp.libraryappcachespring.business.models.Book
import libraryapp.libraryappcachespring.business.models.Cache
import libraryapp.libraryappcachespring.business.models.Content

interface IDeserializerService {
    fun deserialize(databaseResult: Cache): List<Book>
}