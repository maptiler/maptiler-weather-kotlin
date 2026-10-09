/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * An RGBA color representation where components are between 0 and 255.
 */
@Serializable(with = MTRGBAColorSerializer::class)
data class MTRGBAColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
)

object MTRGBAColorSerializer : KSerializer<MTRGBAColor> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MTRGBAColor")

    private const val NOT_SUPPORTED_MESSAGE = "Only JSON format is supported"

    override fun deserialize(decoder: Decoder): MTRGBAColor {
        val jsonDecoder = decoder as? JsonDecoder ?: throw Exception(NOT_SUPPORTED_MESSAGE)
        val jsonElement = jsonDecoder.decodeJsonElement()
        return if (jsonElement is kotlinx.serialization.json.JsonArray) {
            val r = jsonElement[0].jsonPrimitive.int
            val g = jsonElement[1].jsonPrimitive.int
            val b = jsonElement[2].jsonPrimitive.int
            val a = if (jsonElement.size > 3) jsonElement[3].jsonPrimitive.int else 255
            MTRGBAColor(r, g, b, a)
        } else {
            val jsonObj = jsonElement as kotlinx.serialization.json.JsonObject
            val r = jsonObj["red"]?.jsonPrimitive?.int ?: jsonObj["r"]?.jsonPrimitive?.int ?: 0
            val g = jsonObj["green"]?.jsonPrimitive?.int ?: jsonObj["g"]?.jsonPrimitive?.int ?: 0
            val b = jsonObj["blue"]?.jsonPrimitive?.int ?: jsonObj["b"]?.jsonPrimitive?.int ?: 0
            val a = jsonObj["alpha"]?.jsonPrimitive?.int ?: jsonObj["a"]?.jsonPrimitive?.int ?: 255
            MTRGBAColor(r, g, b, a)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: MTRGBAColor,
    ) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw Exception(NOT_SUPPORTED_MESSAGE)
        val array =
            buildJsonArray {
                add(value.red)
                add(value.green)
                add(value.blue)
                add(value.alpha)
            }
        jsonEncoder.encodeJsonElement(array)
    }
}
