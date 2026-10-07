/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A layer that displays pressure data.
 */
@Serializable
class MTPressureLayer(
    override var identifier: String = "MapTiler Pressure",
    @kotlinx.serialization.Transient
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE),
    @kotlinx.serialization.Transient
    var customOpacity: Double = 1.0,
    @kotlinx.serialization.Transient
    var customSmooth: Boolean = true,
) : MTWeatherLayer(identifier) {
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
        val result = super.pickAt(lng, lat) as? JsonObject ?: return null
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
