package jobs.procrush.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class LegacyPersonalityTrait(
    val label: String,
    @SerialName("scale_position") val scalePosition: Double,
    @SerialName("left_pole") val leftPole: String,
    @SerialName("right_pole") val rightPole: String,
    @SerialName("is_top_strength") val isTopStrength: Boolean = false,
    val details: PersonalityTraitDetails? = null,
)

private fun JsonDecoder.decodeTraitsList(element: JsonElement, expectedCount: Int): List<PersonalityTrait> =
    when (element) {
        is JsonArray -> {
            val list = json.decodeFromJsonElement(ListSerializer(PersonalityTrait.serializer()), element)
            require(list.size == expectedCount) {
                "traits должен содержать ровно $expectedCount элемент(ов), получено ${list.size}"
            }
            list
        }
        is JsonObject -> {
            val legacyTraits =
                json.decodeFromJsonElement(
                    MapSerializer(String.serializer(), LegacyPersonalityTrait.serializer()),
                    element,
                )
            val ordered =
                legacyTraits.entries
                    .sortedWith(
                        compareBy<Map.Entry<String, LegacyPersonalityTrait>> { !it.value.isTopStrength }
                            .thenBy { it.key },
                    )
                    .map { (_, legacy) ->
                        PersonalityTrait(
                            label = legacy.label,
                            scalePosition = legacy.scalePosition,
                            leftPole = legacy.leftPole,
                            rightPole = legacy.rightPole,
                            details = legacy.details,
                        )
                    }
            require(ordered.size == expectedCount) {
                "traits должен содержать ровно $expectedCount элемент(ов), получено ${ordered.size}"
            }
            ordered
        }
        else -> error("traits должен быть массивом или объектом")
    }

private fun JsonEncoder.encodeTraitsArray(traits: List<PersonalityTrait>) {
    val array = json.encodeToJsonElement(ListSerializer(PersonalityTrait.serializer()), traits)
    encodeJsonElement(array)
}

object ConnectionsTraitsSerializer : KSerializer<ConnectionsTraits> {
    override val descriptor: SerialDescriptor = ConnectionsTraits.serializer().descriptor

    override fun deserialize(decoder: Decoder): ConnectionsTraits {
        val jsonDecoder = decoder as JsonDecoder
        val traits = jsonDecoder.decodeTraitsList(jsonDecoder.decodeJsonElement(), 4)
        return ConnectionsTraits(traits[0], traits[1], traits[2], traits[3])
    }

    override fun serialize(encoder: Encoder, value: ConnectionsTraits) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeTraitsArray(value.asList())
    }
}

object CreativityTraitsSerializer : KSerializer<CreativityTraits> {
    override val descriptor: SerialDescriptor = CreativityTraits.serializer().descriptor

    override fun deserialize(decoder: Decoder): CreativityTraits {
        val jsonDecoder = decoder as JsonDecoder
        val traits = jsonDecoder.decodeTraitsList(jsonDecoder.decodeJsonElement(), 3)
        return CreativityTraits(traits[0], traits[1], traits[2])
    }

    override fun serialize(encoder: Encoder, value: CreativityTraits) {
        (encoder as JsonEncoder).encodeTraitsArray(value.asList())
    }
}

object DriveTraitsSerializer : KSerializer<DriveTraits> {
    override val descriptor: SerialDescriptor = DriveTraits.serializer().descriptor

    override fun deserialize(decoder: Decoder): DriveTraits {
        val jsonDecoder = decoder as JsonDecoder
        val traits = jsonDecoder.decodeTraitsList(jsonDecoder.decodeJsonElement(), 4)
        return DriveTraits(traits[0], traits[1], traits[2], traits[3])
    }

    override fun serialize(encoder: Encoder, value: DriveTraits) {
        (encoder as JsonEncoder).encodeTraitsArray(value.asList())
    }
}

object ThinkingTraitsSerializer : KSerializer<ThinkingTraits> {
    override val descriptor: SerialDescriptor = ThinkingTraits.serializer().descriptor

    override fun deserialize(decoder: Decoder): ThinkingTraits {
        val jsonDecoder = decoder as JsonDecoder
        val traits = jsonDecoder.decodeTraitsList(jsonDecoder.decodeJsonElement(), 1)
        return ThinkingTraits(traits[0])
    }

    override fun serialize(encoder: Encoder, value: ThinkingTraits) {
        (encoder as JsonEncoder).encodeTraitsArray(value.asList())
    }
}

object SucceedThroughSerializer : KSerializer<SucceedThrough> {
    override val descriptor: SerialDescriptor = SucceedThrough.serializer().descriptor

    override fun deserialize(decoder: Decoder): SucceedThrough {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        val points =
            when (element) {
                is JsonArray -> {
                    val list = element.map { it.jsonPrimitive.content }
                    require(list.size == PersonalityTraitDetailsRules.SUCCEED_THROUGH_SIZE) {
                        "succeed_through должен содержать ровно ${PersonalityTraitDetailsRules.SUCCEED_THROUGH_SIZE} пункта"
                    }
                    list
                }
                is JsonObject -> error("succeed_through должен быть массивом строк")
                else -> error("succeed_through должен быть массивом")
            }
        return SucceedThrough(points[0], points[1], points[2])
    }

    override fun serialize(encoder: Encoder, value: SucceedThrough) {
        val jsonEncoder = encoder as JsonEncoder
        val array =
            JsonArray(
                listOf(
                    JsonPrimitive(value.point0),
                    JsonPrimitive(value.point1),
                    JsonPrimitive(value.point2),
                ),
            )
        jsonEncoder.encodeJsonElement(array)
    }
}

object EnergySourcesItemsSerializer : KSerializer<EnergySourcesItems> {
    override val descriptor: SerialDescriptor = EnergySourcesItems.serializer().descriptor

    override fun deserialize(decoder: Decoder): EnergySourcesItems {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        val items =
            jsonDecoder.json.decodeFromJsonElement(
                ListSerializer(PersonalityItem.serializer()),
                element,
            )
        require(items.size == PersonalitySectionRules.ENERGY_SOURCES_COUNT) {
            "energy_sources.items должен содержать ровно ${PersonalitySectionRules.ENERGY_SOURCES_COUNT} элемента"
        }
        return EnergySourcesItems(items[0], items[1], items[2])
    }

    override fun serialize(encoder: Encoder, value: EnergySourcesItems) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(
            jsonEncoder.json.encodeToJsonElement(ListSerializer(PersonalityItem.serializer()), value.asList()),
        )
    }
}

object StopFactorsItemsSerializer : KSerializer<StopFactorsItems> {
    override val descriptor: SerialDescriptor = StopFactorsItems.serializer().descriptor

    override fun deserialize(decoder: Decoder): StopFactorsItems {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        val items =
            jsonDecoder.json.decodeFromJsonElement(
                ListSerializer(PersonalityItem.serializer()),
                element,
            )
        require(items.size == PersonalitySectionRules.STOP_FACTORS_COUNT) {
            "stop_factors.items должен содержать ровно ${PersonalitySectionRules.STOP_FACTORS_COUNT} элемента"
        }
        return StopFactorsItems(items[0], items[1])
    }

    override fun serialize(encoder: Encoder, value: StopFactorsItems) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(
            jsonEncoder.json.encodeToJsonElement(ListSerializer(PersonalityItem.serializer()), value.asList()),
        )
    }
}
