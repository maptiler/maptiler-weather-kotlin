/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilersdk.bridge.MTCommand
import kotlinx.coroutines.launch
import java.util.Date

/**
 * The TileLayer is the base class for weather layers that consist of multiple timeframes smoothly animated over time.
 */
open class MTTileLayer(
    identifier: String,
    sourceIdentifier: String = "",
) : MTWeatherLayer(identifier, sourceIdentifier) {
    override val jsClassName: String
        get() = "TileLayer"

    /**
     * Adds a new timeframe/frame to the animation.
     *
     * @param time The time of the timeframe.
     * @param url The URL of the tile source.
     */
    fun addSource(
        time: Date,
        url: String,
    ) {
        addSource(time.time / 1000.0, url)
    }

    /**
     * Adds a new timeframe/frame to the animation using a Unix timestamp in seconds.
     *
     * @param timeSeconds The time of the timeframe in seconds.
     * @param url The URL of the tile source.
     */
    fun addSource(
        timeSeconds: Double,
        url: String,
    ) {
        val controller = this.controller ?: return
        controller.scope?.launch {
            val command =
                object : MTCommand {
                    override val isPrimitiveReturnType: Boolean = true

                    override fun toJS(): String =
                        """
                        (function() {
                            var styleLayer = map.getLayer("${this@MTTileLayer.identifier}");
                            var layer = styleLayer ? (styleLayer.implementation || styleLayer) : null;
                            if (layer && typeof layer.addSource === 'function') {
                                layer.addSource($timeSeconds, "$url");
                            }
                        })();
                        """.trimIndent()
                }
            controller.execute(command)
        }
    }

    /**
     * Removes a specific timeframe.
     *
     * @param time The time of the timeframe to remove.
     */
    fun removeSource(time: Date) {
        removeSource(time.time / 1000.0)
    }

    /**
     * Removes a specific timeframe using a Unix timestamp in seconds.
     *
     * @param timeSeconds The time of the timeframe to remove in seconds.
     */
    fun removeSource(timeSeconds: Double) {
        val controller = this.controller ?: return
        controller.scope?.launch {
            val command =
                object : MTCommand {
                    override val isPrimitiveReturnType: Boolean = true

                    override fun toJS(): String =
                        """
                        (function() {
                            var styleLayer = map.getLayer("${this@MTTileLayer.identifier}");
                            var layer = styleLayer ? (styleLayer.implementation || styleLayer) : null;
                            if (layer && typeof layer.removeSource === 'function') {
                                layer.removeSource($timeSeconds);
                            }
                        })();
                        """.trimIndent()
                }
            controller.execute(command)
        }
    }
}
