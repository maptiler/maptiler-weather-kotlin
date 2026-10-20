/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The Intensity layer is a specialized implementation of a TileLayer configured
 * to display scalar weather data (like precipitation, temperature, or radar) using coloring fragments.
 */
open class MTIntensityLayer(
    identifier: String,
    sourceIdentifier: String = "",
) : MTTileLayer(identifier, sourceIdentifier) {
    override val jsClassName: String
        get() = "Intensity"

    /**
     * Picks weather data at a specific location.
     *
     * @param lng Longitude.
     * @param lat Latitude.
     * @return The weather data value at the given location as a MTIntensityValue, or null if not available.
     */
    override suspend fun pickAt(
        lng: Double,
        lat: Double,
    ): Any? {
        val result = pickAtRaw(lng, lat) ?: return null
        val value = result["value"]?.jsonPrimitive?.doubleOrNull ?: return null
        return MTIntensityValue(value = value)
    }

    override fun opacity(value: Double): MTIntensityLayer = super.opacity(value) as MTIntensityLayer

    override fun smooth(value: Boolean): MTIntensityLayer = super.smooth(value) as MTIntensityLayer

    override fun timeInterpolation(value: Boolean): MTIntensityLayer = super.timeInterpolation(value) as MTIntensityLayer

    override fun colorRamp(value: MTWeatherColorRamp): MTIntensityLayer = super.colorRamp(value) as MTIntensityLayer

    override fun beforeId(value: String?): MTIntensityLayer = super.beforeId(value) as MTIntensityLayer
}

/**
 * Intensity value at a given location.
 */
data class MTIntensityValue(
    /**
     * The decoded value.
     */
    val value: Double,
)
