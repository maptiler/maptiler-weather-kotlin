/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather.commands

import com.maptiler.maptilersdk.bridge.MTBridge
import com.maptiler.maptilersdk.bridge.MTCommand

internal class RemoveWeatherLayer(
    private val layerId: String,
) : MTCommand {
    override val isPrimitiveReturnType: Boolean = false

    override fun toJS(): String {
        return """
            (function() {
                if (${MTBridge.MAP_OBJECT} && ${MTBridge.MAP_OBJECT}.getLayer('$layerId')) {
                    ${MTBridge.MAP_OBJECT}.removeLayer('$layerId');
                }
            })();
            """.trimIndent()
    }
}
