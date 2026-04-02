package libraryapp.libraryappcachespring.business.services

import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IAPIResultService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.ICacheService
import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IFormatterService
import libraryapp.libraryappcachespring.business.models.Cache
import org.springframework.stereotype.Service

@Service
class APIResultService (
    private val formatterService: IFormatterService,
    private val cacheService: ICacheService
) : IAPIResultService {

    override fun fromHTMLAdd(html: String, query: String) {
        val books = formatterService.fromHtml(html)
            .joinToString("~~")
        cacheService.addCache(
            Cache (
                0,
                query,
                books
            )
        )
    }

    override fun fromHTMLUpdate(html: String, query: String) {
        val books = formatterService.fromHtml(html)
            .joinToString("~~")
        cacheService.updateCache(
            Cache (
                0,
                query,
                books
            )
        )
    }

    override fun fromJSONAdd(json: String, query: String) {
        val books = formatterService.fromJSON(json)
            .joinToString("~~")
        cacheService.addCache(
            Cache (
                0,
                query,
                books
            )
        )
    }

    override fun fromJSONUpdate(json: String, query: String) {
        val books = formatterService.fromJSON(json)
            .joinToString("~~")
        cacheService.updateCache(
            Cache (
                0,
                query,
                books
            )
        )
    }
}