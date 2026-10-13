/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather.commands

import com.maptiler.maptilersdk.bridge.MTCommand

internal sealed class WeatherCommandArgument {
    data class StringArg(val value: String) : WeatherCommandArgument()

    data class DoubleArg(val value: Double) : WeatherCommandArgument()

    data class BoolArg(val value: Boolean) : WeatherCommandArgument()

    val jsRepresentation: String
        get() =
            when (this) {
                is StringArg -> "'$value'"
                is DoubleArg -> "$value"
                is BoolArg -> if (value) "true" else "false"
            }
}

internal data class WeatherLayerMethodCommand(
    val layerId: String,
    val methodName: String,
    val arguments: List<WeatherCommandArgument> = emptyList(),
) : MTCommand {
    override val isPrimitiveReturnType: Boolean = false

    override fun toJS(): String {
        val argsCode = arguments.joinToString(", ") { it.jsRepresentation }

        return """
            (function() {
                var styleLayer = map.getLayer("$layerId");
                var layer = styleLayer ? (styleLayer.implementation || styleLayer) : null;
                if (layer && typeof layer.$methodName === 'function') {
                    layer.$methodName($argsCode);
                }
            })();
            """.trimIndent()
    }
}
