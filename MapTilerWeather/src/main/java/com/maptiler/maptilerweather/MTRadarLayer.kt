/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.serialization.Serializable

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
}
