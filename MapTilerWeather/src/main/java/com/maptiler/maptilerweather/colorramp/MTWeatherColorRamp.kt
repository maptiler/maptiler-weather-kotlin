package com.maptiler.maptilerweather.colorramp

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Implementation of Color Ramp for MapTiler Weather SDK.
 */
@Serializable
data class MTWeatherColorRamp(
    var min: Double,
    var max: Double,
    var stops: List<MTWeatherColorRampStop>,
) {
    /**
     * Initializes a custom weather color ramp with explicit stops.
     */
    constructor(stops: List<MTWeatherColorRampStop>, min: Double? = null, max: Double? = null) : this(
        min = min ?: stops.minOfOrNull { it.value } ?: 0.0,
        max = max ?: stops.maxOfOrNull { it.value } ?: 1.0,
        stops = stops.sortedBy { it.value },
    )

    /**
     * Initializes a color ramp from a meteorological preset.
     */
    constructor(preset: MTWeatherColorRampPreset) : this(stops = preset.stops)

    /**
     * Clones the color ramp.
     */
    fun clone(): MTWeatherColorRamp {
        return MTWeatherColorRamp(min, max, stops.map { it.copy(color = it.color.toList()) })
    }

    /**
     * Returns a ramp reversed in-place or cloned depending on the clone flag.
     */
    fun reverse(clone: Boolean = true): MTWeatherColorRamp {
        val target = if (clone) this.clone() else this
        val count = target.stops.size
        for (i in 0 until count / 2) {
            val oppositeIndex = count - 1 - i
            val tempColor = target.stops[i].color
            target.stops[i].color = target.stops[oppositeIndex].color
            target.stops[oppositeIndex].color = tempColor
        }
        return target
    }

    /**
     * Scales the ramp to the given bounds.
     */
    fun scale(
        newMin: Double,
        newMax: Double,
        clone: Boolean = true,
    ): MTWeatherColorRamp {
        val target = if (clone) this.clone() else this
        if (target.stops.isEmpty()) return target

        val currentMin = target.stops.first().value
        val currentMax = target.stops.last().value
        val currentSpan = currentMax - currentMin
        val newSpan = newMax - newMin

        target.stops.forEach { stop ->
            val normalizedValue = if (currentSpan == 0.0) 0.0 else (stop.value - currentMin) / currentSpan
            stop.value = normalizedValue * newSpan + newMin
        }

        target.min = newMin
        target.max = newMax

        return target
    }

    /**
     * Replaces the stops on the ramp.
     */
    fun setStops(
        newStops: List<MTWeatherColorRampStop>,
        clone: Boolean = true,
    ): MTWeatherColorRamp {
        val target = if (clone) this.clone() else this
        target.stops = newStops.sortedBy { it.value }
        target.min = target.stops.firstOrNull()?.value ?: 0.0
        target.max = target.stops.lastOrNull()?.value ?: 1.0
        return target
    }

    /**
     * Prepends a transparent stop at the beginning of the ramp.
     */
    fun transparentStart(clone: Boolean = true): MTWeatherColorRamp {
        val target = if (clone) this.clone() else this
        if (target.stops.isEmpty()) return target

        val first = target.stops.first()
        val newFirst =
            MTWeatherColorRampStop(
                value = first.value,
                color = listOf(first.color[0], first.color[1], first.color[2], 0),
            )

        val mutableStops = target.stops.toMutableList()
        mutableStops.add(0, newFirst)

        // Push the original first stop slightly up
        if (mutableStops.size > 1) {
            mutableStops[1].value += 0.001
        }

        target.stops = mutableStops
        return target
    }

    /**
     * Returns true if the first stop is transparent.
     */
    val hasTransparentStart: Boolean
        get() = stops.firstOrNull()?.color?.getOrNull(3) == 0

    /**
     * Returns the color at the provided value.
     */
    fun getColor(
        value: Double,
        smooth: Boolean = true,
    ): List<Int> {
        if (stops.isEmpty()) return listOf(0, 0, 0, 255)

        if (value <= stops.first().value) return stops.first().color
        if (value >= stops.last().value) return stops.last().color

        for (i in 0 until stops.size - 1) {
            if (value > stops[i + 1].value) continue

            val colorBefore = stops[i].color
            if (!smooth) return colorBefore

            val valueBefore = stops[i].value
            val valueAfter = stops[i + 1].value
            val colorAfter = stops[i + 1].color

            val beforeRatio = (valueAfter - value) / (valueAfter - valueBefore)
            val afterRatio = 1.0 - beforeRatio

            val redVal = (colorBefore[0] * beforeRatio + colorAfter[0] * afterRatio).roundToInt()
            val greenVal = (colorBefore[1] * beforeRatio + colorAfter[1] * afterRatio).roundToInt()
            val blueVal = (colorBefore[2] * beforeRatio + colorAfter[2] * afterRatio).roundToInt()
            val alphaVal = (colorBefore[3] * beforeRatio + colorAfter[3] * afterRatio).roundToInt()

            return listOf(redVal, greenVal, blueVal, alphaVal)
        }

        return listOf(0, 0, 0, 255)
    }

    /**
     * Returns the color as a CSS-like string (e.g., "rgba(255, 255, 255, 1.0)")
     */
    fun getColorString(
        value: Double,
        smooth: Boolean = true,
    ): String {
        val color = getColor(value, smooth)
        val alpha = color[3] / 255.0
        return "rgba(${color[0]}, ${color[1]}, ${color[2]}, $alpha)"
    }

    companion object {
        /**
         * A fully transparent color ramp.
         *
         * Use this to disable the background heatmap visualization while keeping
         * other elements (like wind particles) visible.
         */
        val NONE =
            MTWeatherColorRamp(
                stops = listOf(MTWeatherColorRampStop(0.0, listOf(0, 0, 0, 0))),
            )

        /**
         * Built-in MapTiler Weather color ramps.
         */
        val builtin = BuiltInRamps
    }

    /**
     * Helper object to access builtin ramps similar to the JS SDK (ColorRamp.builtin.PRESSURE)
     */
    object BuiltInRamps {
        val MAKO_PRECIPITATION get() = MTWeatherColorRamp(MTWeatherColorRampPreset.MAKO_PRECIPITATION)
        val PRECIPITATION get() = MTWeatherColorRamp(MTWeatherColorRampPreset.PRECIPITATION)
        val PRECIPITATION_2 get() = MTWeatherColorRamp(MTWeatherColorRampPreset.PRECIPITATION_2)
        val PRESSURE get() = MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE)
        val PRESSURE_2 get() = MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE_2)
        val PRESSURE_3 get() = MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE_3)
        val PRESSURE_4 get() = MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE_4)
        val PRESSURE_CIVIDIS get() = MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE_CIVIDIS)
        val RADAR get() = MTWeatherColorRamp(MTWeatherColorRampPreset.RADAR)
        val RADAR_CLOUD get() = MTWeatherColorRamp(MTWeatherColorRampPreset.RADAR_CLOUD)
        val RADAR_ROCKET get() = MTWeatherColorRamp(MTWeatherColorRampPreset.RADAR_ROCKET)
        val TEMPERATURE_2 get() = MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_2)
        val TEMPERATURE_3 get() = MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_3)
        val TEMPERATURE_TURBO get() = MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_TURBO)
        val TERRAIN get() = MTWeatherColorRamp(MTWeatherColorRampPreset.TERRAIN)
        val WIND_VIRIDIS get() = MTWeatherColorRamp(MTWeatherColorRampPreset.WIND_VIRIDIS)
        val WIND_ROCKET get() = MTWeatherColorRamp(MTWeatherColorRampPreset.WIND_ROCKET)
    }
}
