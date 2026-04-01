package libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces

import libraryapp.libraryappcachespring.business.models.Book

interface IFormatterService {
    fun fromHtml(html: String): List<Book>
    fun fromJSON(json: String): List<Book>
    fun fromRaw(raw: String): List<Book>
}