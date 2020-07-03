package evans18.realestatebrowser.data.model.local

/**
 * A generic class that contains data and status about loading data.
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T? = null) : Resource<T>(data)
    class Loading<T> : Resource<T>()
    class Error<T>(message: String? = null) : Resource<T>(message = message)
}
