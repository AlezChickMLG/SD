package libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces

import javax.swing.text.html.HTML

interface IAPIResultService {
    fun fromHTMLAdd(html: String)
    fun fromHTMLUpdate(html: String)
}