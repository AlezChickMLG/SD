package libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces

import javax.swing.text.html.HTML

interface IAPIResultService {
    fun fromHTMLAdd(html: String, query: String)
    fun fromHTMLUpdate(html: String, query: String)

    fun fromJSONAdd(json: String, query: String)
    fun fromJSONUpdate(json: String, query: String)
}