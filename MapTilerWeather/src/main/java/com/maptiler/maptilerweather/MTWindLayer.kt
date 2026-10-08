/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A layer that displays wind data.
 */
class MTWindLayer(
    identifier: String = "MapTiler Wind",
    var customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.WIND_VIRIDIS),
    var customOpacity: Double = 1.0,
    var customSmooth: Boolean = true,
) : MTTileLayer(identifier) {
    override val jsClassName: String
        get() = "WindLayer"

    init {
        this.colorRamp = customColorRamp
        this.opacity = customOpacity
        this.smooth = customSmooth
    }

    /**
     * Picks wind data at a specific location.
     *
     * @param lng Longitude.
     * @param lat Latitude.
     * @return Wind data value, or null if not available.
     */
    override suspend fun pickAt(
        lng: Double,
        lat: Double,
    ): MTWindValue? {
        val result = pickAtRaw(lng, lat) ?: return null
        val compassDirection = result["compassDirection"]?.jsonPrimitive?.contentOrNull ?: return null
        val directionAngle = result["directionAngle"]?.jsonPrimitive?.doubleOrNull ?: return null
        val speedFeetPerSecond = result["speedFeetPerSecond"]?.jsonPrimitive?.doubleOrNull ?: return null
        val speedKilometersPerHour = result["speedKilometersPerHour"]?.jsonPrimitive?.doubleOrNull ?: return null
        val speedKnots = result["speedKnots"]?.jsonPrimitive?.doubleOrNull ?: return null
        val speedMetersPerSecond = result["speedMetersPerSecond"]?.jsonPrimitive?.doubleOrNull ?: return null
        val speedMilesPerHour = result["speedMilesPerHour"]?.jsonPrimitive?.doubleOrNull ?: return null
        return MTWindValue(
            compassDirection = compassDirection,
            directionAngle = directionAngle,
            speedFeetPerSecond = speedFeetPerSecond,
            speedKilometersPerHour = speedKilometersPerHour,
            speedKnots = speedKnots,
            speedMetersPerSecond = speedMetersPerSecond,
            speedMilesPerHour = speedMilesPerHour,
        )
    }
}

/**
 * Wind value at a given location.
 */
data class MTWindValue(
    /**
     * Conventional 16 compass direction where the wind comes from.
     */
    val compassDirection: String,
    /**
     * Angular direction in degrees towards which the wind is blowing (0° North, 90° East).
     */
    val directionAngle: Double,
    /**
     * Wind speed in feet per second (ft/s).
     */
    val speedFeetPerSecond: Double,
    /**
     * Wind speed in kilometer per hour (km/h).
     */
    val speedKilometersPerHour: Double,
    /**
     * Wind speed in knots (nautical miles per hour).
     */
    val speedKnots: Double,
    /**
     * Wind speed in meters per second (m/s).
     */
    val speedMetersPerSecond: Double,
    /**
     * Wind speed in miles per hour (mph).
     */
    val speedMilesPerHour: Double,
)
