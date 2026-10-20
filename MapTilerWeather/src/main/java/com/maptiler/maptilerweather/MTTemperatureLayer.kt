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
 * A layer that displays temperature data.
 */
class MTTemperatureLayer(
    identifier: String = "MapTiler Temperature",
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_2),
    var customOpacity: Double = 1.0,
    var customSmooth: Boolean = true,
) : MTIntensityLayer(identifier) {
    override val jsClassName: String
        get() = "TemperatureLayer"

    init {
        this.colorRamp = customColorRamp
        this.opacity = customOpacity
        this.smooth = customSmooth
    }

    /**
     * Picks temperature data at a specific location.
     *
     * @param lng Longitude.
     * @param lat Latitude.
     * @return Temperature value, or null if not available.
     */
    override suspend fun pickAt(
        lng: Double,
        lat: Double,
    ): MTTemperatureValue? {
        val result = pickAtRaw(lng, lat) ?: return null
        val value = result["value"]?.jsonPrimitive?.doubleOrNull ?: return null
        val valueImperial = result["valueImperial"]?.jsonPrimitive?.doubleOrNull ?: return null
        return MTTemperatureValue(value = value, valueImperial = valueImperial)
    }

    override fun opacity(value: Double): MTTemperatureLayer = super.opacity(value) as MTTemperatureLayer

    override fun smooth(value: Boolean): MTTemperatureLayer = super.smooth(value) as MTTemperatureLayer

    override fun timeInterpolation(value: Boolean): MTTemperatureLayer = super.timeInterpolation(value) as MTTemperatureLayer

    override fun colorRamp(value: MTWeatherColorRamp): MTTemperatureLayer = super.colorRamp(value) as MTTemperatureLayer

    override fun beforeId(value: String?): MTTemperatureLayer = super.beforeId(value) as MTTemperatureLayer
}

/**
 * Temperature value at a given location.
 */
data class MTTemperatureValue(
    /**
     * Temperature in degree Celsius (°C).
     */
    val value: Double,
    /**
     * Temperature in degree Fahrenheit (°F).
     */
    val valueImperial: Double,
)
