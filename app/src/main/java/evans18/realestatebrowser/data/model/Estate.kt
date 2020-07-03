package evans18.realestatebrowser.data.model

import android.graphics.Bitmap
import evans18.realestatebrowser.data.network.serializer.DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class Estate(

    @SerialName("id")
    val id: Int,
    @SerialName("image")
    val imagePath: String,
    @SerialName("price")
    val price: Int,
    @SerialName("bedrooms")
    val bedroomCount: Int,
    @SerialName("bathrooms")
    val bathroomCount: Int,
    @SerialName("size")
    val size: Int,
    @SerialName("description")
    val description: String,
    @SerialName("zip")
    val postalCode: String,
    @SerialName("city")
    val city: String,
    @SerialName("latitude")
    val latitude: Double,
    @SerialName("longitude")
    val longitude: Double,
    @Serializable(with = DateSerializer::class)
    @SerialName("createdDate")
    val createdDate: Date

) {
    /**
     * Image object set using [Picasso] library for easy loading later into an [ImageView].
     */
    @Transient
    var image: Bitmap? = null

    /**
     * Check only user-readable strings for contains, ignoring case.
     * Todo: is this string concatenation better than if/else order of which property to check first?
     */
    fun contains(query: String, ignoreCase: Boolean = false): Boolean {
        val queryWords = query.split("\\s+".toRegex())
        for (queryWord in queryWords) {
            if (!"$city $postalCode $price".contains(queryWord, ignoreCase)) { // $description
                return false
            }
        }
        return true
    }
}
