/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather.commands

import com.maptiler.maptilersdk.bridge.MTCommand

internal data class WeatherLayerValueCommand(
    val layerId: String,
    val methodName: String,
) : MTCommand {
    override val isPrimitiveReturnType: Boolean = true

    override fun toJS(): String {
        return """
            (function() {
                var styleLayer = map.getLayer("$layerId");
                var layer = styleLayer ? (styleLayer.implementation || styleLayer) : null;
                if (layer && typeof layer.$methodName === 'function') {
                    return layer.$methodName();
                }
                return null;
            })();
            """.trimIndent()
    }
}
