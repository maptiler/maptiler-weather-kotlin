/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather.commands

import com.maptiler.maptilersdk.bridge.MTBridge
import com.maptiler.maptilersdk.bridge.MTCommand
import com.maptiler.maptilersdk.helpers.JsonConfig
import com.maptiler.maptilerweather.MTWeatherLayer
import kotlinx.serialization.encodeToString

internal data class AddWeatherLayer(
    val layer: MTWeatherLayer,
    val beforeId: String? = null,
) : MTCommand {
    override val isPrimitiveReturnType: Boolean = false

    override fun toJS(): String {
        val layerJSONString =
            when (layer) {
                is com.maptiler.maptilerweather.MTParticleLayer ->
                    JsonConfig.json.encodeToString(
                        com.maptiler.maptilerweather.MTParticleLayer.serializer(),
                        layer,
                    )
                else -> JsonConfig.json.encodeToString(MTWeatherLayer.serializer(), layer)
            }
        val beforeIdJS = if (beforeId != null) "'$beforeId'" else BEFORE_ID_JS_SNIPPET

        return """
            (function() {
                function tryAddLayer(retries) {
                    var weatherNS = window.maptilerweather;

                    if (!weatherNS) {
                        if (retries > 0) {
                            setTimeout(function() { tryAddLayer(retries - 1); }, 100);
                            return;
                        }
                        console.error(
                            "MapTiler Weather: maptilerweather not found. " +
                            "Make sure the module is correctly registered and loaded."
                        );
                        return;
                    }

                    var options = $layerJSONString;

                    if (options.colorramp) {
                        options.colorramp = new weatherNS.ColorRamp(options.colorramp);
                    }

                    if (options.coloring) {
                        if (options.coloring.type === 'gradient') {
                            options.coloring = new weatherNS.GradientColoringFragment(options.coloring);
                        }
                    }

                    // Remove properties not expected by the weather layer options
                    delete options.type;
                    delete options.source;
                    delete options.visibility;

                    var before = $beforeIdJS;

                    // Adjust water opacity if inserting below water
                    if (before === 'Water' || before === 'water') {
                        try {
                            ${MTBridge.MAP_OBJECT}.setPaintProperty(before, 'fill-color', 'rgba(0, 0, 0, 0.4)');
                        } catch (e) {
                            console.error('Failed to set water transparency', e);
                        }
                    }

                    var layer = new weatherNS.${layer.jsClassName}(options);
                    
                    var events = ['sourceReady', 'playAnimation', 'pauseAnimation', 'tick', 'animationTimeSet'];
                    events.forEach(function(eventName) {
                        layer.on(eventName, function(e) {
                            if (window.Android && window.Android.onModuleEvent) {
                                var data = { layerId: "${layer.identifier}" };
                                if (e && e.time !== undefined) {
                                    data.time = e.time;
                                }
                                window.Android.onModuleEvent("maptiler-weather", eventName, JSON.stringify(data));
                            }
                        });
                    });

                    ${MTBridge.MAP_OBJECT}.addLayer(layer, before);
                }
                tryAddLayer(10);
            })();
            """.trimIndent()
    }

    companion object {
        private const val BEFORE_ID_JS_SNIPPET = """
        (function() {
            if (${MTBridge.MAP_OBJECT}.getLayer('Water')) return 'Water';
            if (${MTBridge.MAP_OBJECT}.getLayer('water')) return 'water';
            var layers = ${MTBridge.MAP_OBJECT}.getStyle().layers;
            for (var i = 0; i < layers.length; i++) {
                var id = layers[i].id.toLowerCase();
                if (id.indexOf('boundary') !== -1 || id.indexOf('border') !== -1) {
                    return layers[i].id;
                }
            }
            for (var i = 0; i < layers.length; i++) {
                if (layers[i].type === 'symbol') {
                    return layers[i].id;
                }
            }
            return undefined;
        })()
        """
    }
}
