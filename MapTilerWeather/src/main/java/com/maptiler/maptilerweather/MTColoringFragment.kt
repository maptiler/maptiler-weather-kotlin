/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampStop
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Base interface for all coloring fragments.
 */
@Serializable(with = MTColoringFragmentSerializer::class)
interface MTColoringFragment {
    val type: String
}

/**
 * A coloring fragment that uses a color gradient.
 */
@Serializable
class MTGradientColoringFragment(
    var decode: MTDecoderOptions,
    var stops: List<MTWeatherColorRampStop>,
    var smooth: Boolean = true,
    var opacity: Double = 1.0,
) : MTColoringFragment {
    override val type: String
        get() = "gradient"
}

object MTColoringFragmentSerializer : KSerializer<MTColoringFragment> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MTColoringFragment")

    private const val NOT_SUPPORTED_MESSAGE = "Only JSON format is supported"

    override fun deserialize(decoder: Decoder): MTColoringFragment {
        val jsonDecoder = decoder as? JsonDecoder ?: throw Exception(NOT_SUPPORTED_MESSAGE)
        val jsonElement = jsonDecoder.decodeJsonElement() as JsonObject
        val type = jsonElement["type"]?.jsonPrimitive?.content ?: throw Exception("Missing type field")
        return when (type) {
            "gradient" -> jsonDecoder.json.decodeFromJsonElement(MTGradientColoringFragment.serializer(), jsonElement)
            else -> throw Exception("Unknown coloring fragment type: $type")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: MTColoringFragment,
    ) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw Exception(NOT_SUPPORTED_MESSAGE)
        when (value) {
            is MTGradientColoringFragment -> {
                val element = jsonEncoder.json.encodeToJsonElement(MTGradientColoringFragment.serializer(), value) as JsonObject
                val newObj =
                    buildJsonObject {
                        element.forEach { (key, valElement) ->
                            put(key, valElement)
                        }
                        put("type", "gradient")
                    }
                jsonEncoder.encodeJsonElement(newObj)
            }
            else -> {
                val newObj =
                    buildJsonObject {
                        put("type", value.type)
                    }
                jsonEncoder.encodeJsonElement(newObj)
            }
        }
    }
}
