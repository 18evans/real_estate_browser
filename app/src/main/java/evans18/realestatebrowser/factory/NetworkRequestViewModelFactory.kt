package evans18.realestatebrowser.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import evans18.realestatebrowser.ui.fragment.estate.EstatesViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

object NetworkRequestViewModelFactory : ViewModelProvider.Factory {

    const val BASE_IMAGE_HOSTING_URL = "i.ibb.co"
    const val BASE_URL = "api.jsonbin.io"
    val BASE_URL_PROTOCOL = URLProtocol.HTTPS
    private const val HEADER_ACCESS_KEY = "secret-key"
    private const val HEADER_ACCESS_KEY_VALUE = "\$2b\$10\$7id7DxAFFW3lFE2Eo2FLVOyp0n1VbVfMw4Z8qyD7WiBrVuRw.q1DC"

    private val client by lazy {
        HttpClient(Android) {

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }

            install(JsonFeature) {
                serializer = KotlinxSerializer(
                    Json(
                        JsonConfiguration.Stable.copy(ignoreUnknownKeys = true)
                    )
                )
            }
            defaultRequest {
                url {
                    protocol = BASE_URL_PROTOCOL
                    host = BASE_URL
                }
                header(HEADER_ACCESS_KEY, HEADER_ACCESS_KEY_VALUE)

            }
        }

    }

    /**
     * Append multiple cases for each ViewModel you'd like to use this [NetworkRequestViewModelFactory] for.
     */
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EstatesViewModel::class.java))
            return EstatesViewModel(client) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}