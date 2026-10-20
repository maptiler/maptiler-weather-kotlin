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
                    if (before && !${MTBridge.MAP_OBJECT}.getLayer(before)) {
                        var styleObj = ${MTBridge.MAP_OBJECT}.getStyle();
                        var styleLayers = styleObj ? styleObj.layers : [];
                        before = undefined;
                        for (var i = 0; i < styleLayers.length; i++) {
                            if (styleLayers[i].type === 'symbol') {
                                before = styleLayers[i].id;
                                break;
                            }
                        }
                    }

                    // Remove layer with same ID if already present
                    if (${MTBridge.MAP_OBJECT}.getLayer("${layer.identifier}")) {
                        try {
                            ${MTBridge.MAP_OBJECT}.removeLayer("${layer.identifier}");
                        } catch(e) {}
                    }

                    var layer = new weatherNS.${layer.jsClassName}(options);
                    ${eventForwardingJS("layer", layer.identifier)}

                    ${MTBridge.MAP_OBJECT}.addLayer(layer, before);
                }
                tryAddLayer(10);
            })();
            """.trimIndent()
    }

    companion object {
        /**
         * Minimum interval between `tick` events forwarded to Kotlin.
         *
         * `@JavascriptInterface` calls are synchronous and block the WebView's JS thread,
         * so forwarding every animation frame starves rendering while frames are still loading.
         */
        private const val TICK_FORWARD_INTERVAL_MS = 50

        /**
         * JS that forwards the weather layer events of [layerVar] to the Android bridge.
         */
        internal fun eventForwardingJS(
            layerVar: String,
            layerId: String,
        ): String =
            """
            var lastTickForwardTime = 0;
            ['sourceReady', 'playAnimation', 'pauseAnimation', 'tick', 'animationTimeSet'].forEach(function(eventName) {
                $layerVar.on(eventName, function(e) {
                    if (!window.Android || !window.Android.onModuleEvent) return;
                    if (eventName === 'tick') {
                        var now = performance.now();
                        if (now - lastTickForwardTime < $TICK_FORWARD_INTERVAL_MS) return;
                        lastTickForwardTime = now;
                    }
                    var data = { layerId: "$layerId" };
                    if (e && e.time !== undefined) {
                        data.time = e.time;
                    }
                    window.Android.onModuleEvent("maptiler-weather", eventName, JSON.stringify(data));
                });
            });
            """

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
