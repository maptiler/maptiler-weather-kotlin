/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A layer that displays pressure data.
 */
class MTPressureLayer(
    identifier: String = "MapTiler Pressure",
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE),
    var customOpacity: Double = 1.0,
    var customSmooth: Boolean = true,
) : MTIntensityLayer(identifier) {
    override val jsClassName: String
        get() = "PressureLayer"

    init {
        this.colorRamp = customColorRamp
        this.opacity = customOpacity
        this.smooth = customSmooth
    }

    /**
     * Picks pressure data at a specific location.
     *
     * @param lng Longitude.
     * @param lat Latitude.
     * @return Pressure value in hPa (mbar), or null if not available.
     */
    override suspend fun pickAt(
        lng: Double,
        lat: Double,
    ): MTPressureValue? {
        val result = pickAtRaw(lng, lat) ?: return null
        val value = result["value"]?.jsonPrimitive?.doubleOrNull ?: return null
        return MTPressureValue(value = value)
    }
}

/**
 * Pressure value at a given location.
 */
data class MTPressureValue(
    /**
     * Pressure in hPa (hectopascal) or mbar (millibar).
     */
    val value: Double,
)
