@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.ensodai.avalonmediacard.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.TemplateAction

data class UnknownServerAction(
    val originalType: String,
    val payload: JsonObject
) : ServerAction, TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        val newPayload = buildJsonObject {
            payload.forEach { (k, v) -> put(k, v) }
            when (value) {
                is String -> put(key, JsonPrimitive(value))
                is Number -> put(key, JsonPrimitive(value))
                is Boolean -> put(key, JsonPrimitive(value))
                else -> put(key, JsonPrimitive(value.toString()))
            }
        }
        return copy(payload = newPayload)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Action> createUnknownActionDeserializer(className: String?): KSerializer<T> {
    return object : KSerializer<T> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(className ?: "Unknown")

        override fun deserialize(decoder: Decoder): T {
            val jsonDecoder = decoder as? JsonDecoder ?: error("JSON only")
            val element = jsonDecoder.decodeJsonElement()
            return UnknownServerAction(className ?: "Unknown", element.jsonObject) as T
        }

        override fun serialize(encoder: Encoder, value: T) {
            error("This serializer is only for deserialization")
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Action> createUnknownActionSerializer(originalType: String): KSerializer<T> {
    return object : KSerializer<T> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(originalType)

        override fun serialize(encoder: Encoder, value: T) {
            val jsonEncoder = encoder as? JsonEncoder ?: error("JSON only")
            val action = value as UnknownServerAction
            jsonEncoder.encodeJsonElement(action.payload)
        }

        override fun deserialize(decoder: Decoder): T {
            error("This serializer is only for serialization")
        }
    }
}

object ClientServerActionSerializer : KSerializer<ServerAction> {
    override val descriptor = buildClassSerialDescriptor("ServerAction")

    override fun serialize(encoder: Encoder, value: ServerAction) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("JSON only supported")
        val discriminator = jsonEncoder.json.configuration.classDiscriminator

        if (value is UnknownServerAction) {
            val jsonObject = buildJsonObject {
                put(discriminator, JsonPrimitive(value.originalType))
                value.payload.forEach { (k, v) ->
                    if (k != discriminator) put(k, v)
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        } else {
            val serializer = encoder.serializersModule.getPolymorphic(ServerAction::class, value)
                ?: error("Serializer not found for ${value::class}")

            @Suppress("UNCHECKED_CAST")
            val jsonElement = jsonEncoder.json.encodeToJsonElement(
                serializer as KSerializer<Any>,
                value as Any
            )

            val className = serializer.descriptor.serialName

            val jsonObject = buildJsonObject {
                put(discriminator, JsonPrimitive(className))
                if (jsonElement is JsonObject) {
                    jsonElement.forEach { (k, v) -> if (k != discriminator) put(k, v) }
                }
            }

            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }

    override fun deserialize(decoder: Decoder): ServerAction {
        error("This is only used for top-level serialization")
    }
}
