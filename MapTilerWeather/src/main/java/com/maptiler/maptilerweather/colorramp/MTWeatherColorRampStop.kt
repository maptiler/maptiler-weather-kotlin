package com.maptiler.maptilerweather.colorramp

import kotlinx.serialization.Serializable

/**
 * A stop in a weather color ramp, pairing a value with an RGBA color.
 */
@Serializable
data class MTWeatherColorRampStop(
    var value: Double,
    var color: List<Int>,
) {
    init {
        require(color.size == 4) { "Color stop must contain exactly 4 channels (RGBA)." }
        require(color.all { it in 0..255 }) { "Color channel values must be between 0 and 255." }
    }
}
