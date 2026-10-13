/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilersdk.bridge.MTCommand
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A layer that displays animated particles for directional weather data.
 *
 * This layer serves as the base for particle-based animations, such as wind or ocean currents.
 */
@Serializable
open class MTParticleLayer() : MTTileLayer("MapTiler ParticleLayer", "") {
    override val jsClassName: String
        get() = "ParticleLayer"

    init {
        this.colorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.WIND_VIRIDIS)
        this.opacity = 1.0
        this.smooth = true
    }

    constructor(
        identifier: String = "MapTiler ParticleLayer",
        customColorRamp: MTWeatherColorRamp = MTWeatherColorRamp(MTWeatherColorRampPreset.WIND_VIRIDIS),
        customOpacity: Double = 1.0,
        customSmooth: Boolean = true,
    ) : this() {
        this.identifier = identifier
        this.colorRamp = customColorRamp
        this.opacity = customOpacity
        this.smooth = customSmooth
    }

    /**
     * Color of the particle. RGBA 0-255.
     */
    var color: MTRGBAColor? = MTRGBAColor(255, 255, 255, 192)

    /**
     * Color of the particle when moving "fast". RGBA 0-255.
     */
    @SerialName("fastColor")
    var fastColor: MTRGBAColor? = null

    /**
     * Number of particles visible per 1000 px^2.
     */
    var density: Double? = 2.0

    /**
     * How much the particles fade over time.
     */
    @SerialName("fadeFactor")
    var fadeFactor: Double? = 0.1

    /**
     * What is considered "fast" (in px/sec) for coloring purposes.
     */
    @SerialName("fastSpeed")
    var fastSpeed: Double? = null

    /**
     * Quantity of particles to be created. Has to be a power of 2 and at least 4.
     */
    @SerialName("maxAmount")
    var maxAmount: Int? = 128

    /**
     * Use more pixels to make particles more smooth (especially when tilted).
     */
    @SerialName("pixelRatio")
    var pixelRatio: Double? = null

    /**
     * Time interval (in milliseconds) how often the particles are refreshed to avoid degradation.
     */
    @SerialName("refreshInterval")
    var refreshInterval: Double? = 800.0

    /**
     * Size of the particle.
     */
    var size: Double? = 1.5

    /**
     * Speed factor of the particles.
     */
    var speed: Double? = 0.001

    /**
     * If this is true, the particles gets slightly larger as they become faster.
     */
    @SerialName("fastIsLarger")
    var fastIsLarger: Boolean? = false

    // MARK: - DSL Modifiers

    /**
     * Modifier. Sets the color.
     */
    fun color(value: MTRGBAColor): MTParticleLayer {
        this.color = value
        return this
    }

    /**
     * Modifier. Sets the fastColor.
     */
    fun fastColor(value: MTRGBAColor): MTParticleLayer {
        this.fastColor = value
        return this
    }

    /**
     * Modifier. Sets the density.
     */
    fun density(value: Double): MTParticleLayer {
        this.density = value
        return this
    }

    /**
     * Modifier. Sets the fadeFactor.
     */
    fun fadeFactor(value: Double): MTParticleLayer {
        this.fadeFactor = value
        return this
    }

    /**
     * Modifier. Sets the fastSpeed.
     */
    fun fastSpeed(value: Double): MTParticleLayer {
        this.fastSpeed = value
        return this
    }

    /**
     * Modifier. Sets the maxAmount.
     */
    fun maxAmount(value: Int): MTParticleLayer {
        this.maxAmount = value
        return this
    }

    /**
     * Modifier. Sets the pixelRatio.
     */
    fun pixelRatio(value: Double): MTParticleLayer {
        this.pixelRatio = value
        return this
    }

    /**
     * Modifier. Sets the refreshInterval.
     */
    fun refreshInterval(value: Double): MTParticleLayer {
        this.refreshInterval = value
        return this
    }

    /**
     * Modifier. Sets the size.
     */
    fun size(value: Double): MTParticleLayer {
        this.size = value
        return this
    }

    /**
     * Modifier. Sets the speed.
     */
    fun speed(value: Double): MTParticleLayer {
        this.speed = value
        return this
    }

    /**
     * Modifier. Sets the fastIsLarger.
     */
    fun fastIsLarger(value: Boolean): MTParticleLayer {
        this.fastIsLarger = value
        return this
    }

    // MARK: - Dynamic Update Methods

    /**
     * Updates all particle physics and visual configurations at once without recreating the layer.
     */
    fun updateParticleOptions() {
        val controller = this.controller ?: return
        controller.scope?.launch {
            val command =
                object : MTCommand {
                    override val isPrimitiveReturnType: Boolean = true

                    override fun toJS(): String {
                        val layerJSONString =
                            com.maptiler.maptilersdk.helpers.JsonConfig.json.encodeToString(
                                serializer(),
                                this@MTParticleLayer,
                            )
                        return """
                            (function() {
                                var styleLayer = map.getLayer("${this@MTParticleLayer.identifier}");
                                var layer = styleLayer ? (styleLayer.implementation || styleLayer) : null;
                                if (layer && typeof layer.updateParticleOptions === 'function') {
                                    var options = $layerJSONString;
                                    if (options.colorramp) {
                                        options.colorramp = new window.maptilerweather.ColorRamp(options.colorramp);
                                    }
                                    layer.updateParticleOptions(options);
                                } else {
                                    var currentTime = layer && typeof layer.getAnimationTime === 'function' ? layer.getAnimationTime() : null;
                                    var currentSpeed = layer && typeof layer.getAnimationSpeed === 'function' ? layer.getAnimationSpeed() : null;
                                    
                                    if (styleLayer) {
                                        map.removeLayer("${this@MTParticleLayer.identifier}");
                                    }
                                    
                                    var options = $layerJSONString;
                                    if (options.colorramp) {
                                        options.colorramp = new window.maptilerweather.ColorRamp(options.colorramp);
                                    }
                                    delete options.type;
                                    delete options.source;
                                    delete options.visibility;
                                    
                                    var newLayer = new window.maptilerweather.ParticleLayer(options);
                                    
                                    var events = ['sourceReady', 'playAnimation', 'pauseAnimation', 'tick', 'animationTimeSet'];
                                    events.forEach(function(eventName) {
                                        newLayer.on(eventName, function(e) {
                                            if (window.Android && window.Android.onModuleEvent) {
                                                var data = { layerId: "${this@MTParticleLayer.identifier}" };
                                                if (e && e.time !== undefined) {
                                                    data.time = e.time;
                                                }
                                                window.Android.onModuleEvent("maptiler-weather", eventName, JSON.stringify(data));
                                            }
                                        });
                                    });
                                    
                                    var beforeId = ${if (beforeId != null) "'$beforeId'" else "undefined"};
                                    map.addLayer(newLayer, beforeId);
                                    
                                    if (currentTime !== null && typeof newLayer.setAnimationTime === 'function') {
                                        newLayer.setAnimationTime(currentTime);
                                    }
                                    if (currentSpeed !== null && typeof newLayer.animateByFactor === 'function') {
                                        newLayer.animateByFactor(currentSpeed);
                                    }
                                }
                            })();
                            """.trimIndent()
                    }
                }
            controller.execute(command)
        }
    }
}
