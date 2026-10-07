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
 * A layer that displays radar data.
 */
@Serializable
class MTRadarLayer(
    override var identifier: String = "MapTiler Radar",
    @kotlinx.serialization.Transient
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.RADAR),
    @kotlinx.serialization.Transient
    var customOpacity: Double = 1.0,
    @kotlinx.serialization.Transient
    var customSmooth: Boolean = true,
) : MTWeatherLayer(identifier) {
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
        val result = super.pickAt(lng, lat) as? JsonObject ?: return null
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
