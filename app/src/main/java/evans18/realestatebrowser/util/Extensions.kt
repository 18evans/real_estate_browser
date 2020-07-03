package evans18.realestatebrowser.util

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.NumberFormat

/**
 * Example: 1000 -> "1,000"
 */
val Int.commaString: String
    get() = NumberFormat.getInstance().format(this)

suspend fun <A> Iterable<A>.parallelForEach(operation: suspend (A) -> Unit) = coroutineScope {
    map { async { operation(it) } }.awaitAll()
}