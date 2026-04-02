package libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces

interface IWebService {
    fun webClient()
    fun getPrint(format: String): String?
    fun getFind(filterName: String, filter: String): String?
}