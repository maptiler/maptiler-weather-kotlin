/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather.commands

import com.maptiler.maptilersdk.bridge.MTBridge
import com.maptiler.maptilersdk.bridge.MTCommand

/**
 * Enables or disables forwarding of the layer's `tick` events to Kotlin.
 */
internal data class SetTickForwarding(
    val layerId: String,
    val enabled: Boolean,
) : MTCommand {
    override val isPrimitiveReturnType: Boolean = false

    override fun toJS(): String {
        return """
            (function() {
                var styleLayer = ${MTBridge.MAP_OBJECT}.getLayer("$layerId");
                var layer = styleLayer ? (styleLayer.implementation || styleLayer) : null;
                if (layer) {
                    layer.${AddWeatherLayer.FORWARD_TICK_PROPERTY} = $enabled;
                }
            })();
            """.trimIndent()
    }
}
