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
 * A layer that displays radar data.
 */
class MTRadarLayer(
    identifier: String = "MapTiler Radar",
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.RADAR),
    var customOpacity: Double = 1.0,
    var customSmooth: Boolean = true,
) : MTIntensityLayer(identifier) {
    override val jsClassName: String
        get() = "RadarLayer"

    init {
        this.colorRamp = customColorRamp
        this.opacity = customOpacity
        this.smooth = customSmooth
    }

    /**
     * Picks radar data at a specific location.
     *
     * @param lng Longitude.
     * @param lat Latitude.
     * @return Radar reflectivity value in dBZ, or null if not available.
     */
    override suspend fun pickAt(
        lng: Double,
        lat: Double,
    ): MTRadarValue? {
        val result = pickAtRaw(lng, lat) ?: return null
        val value = result["value"]?.jsonPrimitive?.doubleOrNull ?: return null
        return MTRadarValue(value = value)
    }
}

/**
 * Radar reflectivity value at a given location.
 */
data class MTRadarValue(
    /**
     * Radar reflectivity in dBZ.
     */
    val value: Double,
)
