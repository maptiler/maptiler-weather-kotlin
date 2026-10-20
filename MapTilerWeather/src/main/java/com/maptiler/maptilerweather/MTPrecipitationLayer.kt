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
 * A layer that displays precipitation data.
 */
class MTPrecipitationLayer(
    identifier: String = "MapTiler Precipitation",
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.PRECIPITATION),
    var customOpacity: Double = 1.0,
    var customSmooth: Boolean = true,
) : MTIntensityLayer(identifier) {
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
        val result = pickAtRaw(lng, lat) ?: return null
        val value = result["value"]?.jsonPrimitive?.doubleOrNull ?: return null
        return MTPrecipitationValue(value = value)
    }

    override fun opacity(value: Double): MTPrecipitationLayer = super.opacity(value) as MTPrecipitationLayer

    override fun smooth(value: Boolean): MTPrecipitationLayer = super.smooth(value) as MTPrecipitationLayer

    override fun timeInterpolation(value: Boolean): MTPrecipitationLayer = super.timeInterpolation(value) as MTPrecipitationLayer

    override fun colorRamp(value: MTWeatherColorRamp): MTPrecipitationLayer = super.colorRamp(value) as MTPrecipitationLayer

    override fun beforeId(value: String?): MTPrecipitationLayer = super.beforeId(value) as MTPrecipitationLayer
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
