package libraryapp.libraryappcachespring.business.models

data class Content(
    var author: String?,
    var text: String?,
    var name: String?,
    var publisher: String?
) {
    override fun toString(): String {
        return "author:$author~text:$text~name:$name~publisher:$publisher"
    }
}