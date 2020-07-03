package evans18.realestatebrowser.data.network.serializer

import kotlinx.serialization.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)

    override val descriptor: SerialDescriptor = PrimitiveDescriptor("DateSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(formatter.format(value))

    override fun deserialize(decoder: Decoder): Date {
        val decodeString = decoder.decodeString()

        try {
            return formatter.parse(decodeString)!!
        } catch (ex: ParseException) {
            throw SerializationException("Couldn't decode date from string : \"$decodeString\"")
        } catch (ex: NullPointerException) {
            throw SerializationException("Couldn't decode date from string : \"$decodeString\"")
        }
    }

}