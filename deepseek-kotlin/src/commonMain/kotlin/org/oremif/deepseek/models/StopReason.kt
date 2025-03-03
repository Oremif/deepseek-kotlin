package org.oremif.deepseek.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

@Serializable(with = StopReasoningSerializer::class)
public class StopReason(
    public val reasons: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StopReason) return false
        return reasons == other.reasons
    }

    override fun hashCode(): Int {
        return reasons.hashCode()
    }

    override fun toString(): String =
        "StopReason(reasons=$reasons)"
}


internal object StopReasoningSerializer : KSerializer<StopReason> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StopReasoning", PrimitiveKind.STRING)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: StopReason) {
        when {
            value.reasons.size == 1 -> encoder.encodeString(value.reasons[0])
            value.reasons.size > 1 -> encoder.encodeSerializableValue(
                ListSerializer(String.serializer()),
                value.reasons
            )

            value.reasons.isEmpty() -> encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): StopReason {
        val jsonDecoder =
            decoder as? JsonDecoder ?: throw IllegalStateException("Only JSON is supported for StopReasoningSerializer")

        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> StopReason(listOf(element.content))
            is JsonArray -> StopReason(
                jsonDecoder.json.decodeFromJsonElement(
                    ListSerializer(String.serializer()),
                    element
                )
            )

            else -> StopReason(listOf(element.toString()))
        }
    }

}