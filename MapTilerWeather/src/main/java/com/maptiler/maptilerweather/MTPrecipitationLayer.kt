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
 * A layer that displays precipitation data.
 */
@Serializable
class MTPrecipitationLayer(
    override var identifier: String = "MapTiler Precipitation",
    @kotlinx.serialization.Transient
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.PRECIPITATION),
    @kotlinx.serialization.Transient
    var customOpacity: Double = 1.0,
    @kotlinx.serialization.Transient
    var customSmooth: Boolean = true,
) : MTWeatherLayer(identifier) {
    override val jsClassName: String
        get() = "PrecipitationLayer"

    init {
        this.colorRamp = customColorRamp
        this.opacity = customOpacity
        this.smooth = customSmooth
    }

    /**
     * Picks precipitation data at a specific location.
     *
     * @param lng Longitude.
     * @param lat Latitude.
     * @return Precipitation value in mm/h, or null if not available.
     */
    override suspend fun pickAt(
        lng: Double,
        lat: Double,
    ): MTPrecipitationValue? {
        val result = super.pickAt(lng, lat) as? JsonObject ?: return null
        val value = result["value"]?.jsonPrimitive?.doubleOrNull ?: return null
        return MTPrecipitationValue(value = value)
    }
}

/**
 * Precipitation value at a given location.
 */
data class MTPrecipitationValue(
    /**
     * Precipitation in mm/h.
     */
    val value: Double,
)
