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
                function doAddLayer() {
                    var weatherNS = window.maptilerweather;
                    if (!weatherNS) {
                        console.error("MapTiler Weather: maptilerweather not found.");
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
                    ${eventForwardingJS("layer", layer.identifier, layer.onTick != null)}
                    ${bufferedPlaybackJS("layer")}

                    ${MTBridge.MAP_OBJECT}.addLayer(layer, before);
                }
                doAddLayer();
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
        internal const val TICK_FORWARD_INTERVAL_MS = 100

        /**
         * Number of frames past the current one whose tiles are requested ahead of playback.
         */
        internal const val PREFETCH_FRAMES_AHEAD = 4

        /**
         * Maximum time the animation clock is held waiting for one frame pair to load.
         * Stops a tile that never loads (e.g. a failed request) from stalling playback forever.
         */
        internal const val MAX_BUFFER_HOLD_MS = 3000

        /**
         * JS property on the weather layer telling whether `tick` events are forwarded to Kotlin.
         */
        internal const val FORWARD_TICK_PROPERTY = "_mtForwardTick"

        /**
         * JS that forwards the weather layer events of [layerVar] to the Android bridge.
         *
         * `tick` events are only forwarded when [forwardTick] is true (a Kotlin `onTick` is set);
         * toggle it later through [FORWARD_TICK_PROPERTY].
         */
        internal fun eventForwardingJS(
            layerVar: String,
            layerId: String,
            forwardTick: Boolean,
        ): String =
            """
            var lastTickForwardTime = 0;
            $layerVar.$FORWARD_TICK_PROPERTY = $forwardTick;
            ['sourceReady', 'playAnimation', 'pauseAnimation', 'tick', 'animationTimeSet'].forEach(function(eventName) {
                $layerVar.on(eventName, function(e) {
                    if (!window.Android || !window.Android.onModuleEvent) return;
                    if (eventName === 'tick') {
                        if (!$layerVar.$FORWARD_TICK_PROPERTY) return;
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

        /**
         * JS that makes the animation of [layerVar] wait for its data instead of skipping ahead.
         *
         * The weather library advances animation time regardless of loaded tiles and prefetches a single
         * frame ahead, so on slow networks it keeps showing stale frames and then jumps several hours.
         * This holds the clock until the current frame pair is loaded (for at most [MAX_BUFFER_HOLD_MS])
         * and requests tiles [PREFETCH_FRAMES_AHEAD] frames ahead. It relies on internals of the bundled
         * `maptiler-weather.js` and leaves the animation untouched if any of them is missing.
         */
        internal fun bufferedPlaybackJS(layerVar: String): String =
            """
            (function(l) {
                if (typeof l.animationTick !== 'function' || typeof l.getCurrentFrames !== 'function' ||
                    typeof l.getNextFrame !== 'function' || typeof l.getWantedTiles !== 'function') {
                    return;
                }
                var originalTick = l.animationTick;
                var waitedPair = null;
                var waitStart = 0;
                var skippedPair = null;

                function wantedKeys(layer) {
                    var grid = layer.tilegrid;
                    var wanted = layer.getWantedTiles(${MTBridge.MAP_OBJECT}.getZoom(), grid.getMinZoom(), grid.getMaxZoom());
                    var keys = {};
                    Object.keys(wanted).forEach(function(key) {
                        var zxy = key.split('/').map(Number);
                        var n = Math.pow(2, zxy[0]);
                        if (zxy[2] < 0 || zxy[2] >= n) return;
                        keys[zxy[0] + '/' + (((zxy[1] % n) + n) % n) + '/' + zxy[2]] = true;
                    });
                    return Object.keys(keys);
                }

                function isLoaded(frame, keys) {
                    return keys.every(function(key) {
                        var tile = frame.data.getTile(key, false);
                        return tile && tile.ready;
                    });
                }

                l.animationTick = function() {
                    try {
                        if (this.animationSpeed > 0 && this.frames && this.frames.length > 1 && this.tilegrid) {
                            var keys = wantedKeys(this);
                            var current = this.getCurrentFrames();
                            var frame = current.frameA;
                            for (var i = 0; i <= $PREFETCH_FRAMES_AHEAD && frame; i++) {
                                for (var k = 0; k < keys.length; k++) {
                                    frame.data.getTile(keys[k], true);
                                }
                                frame = this.getNextFrame(frame, 1) || this.frames[0];
                            }

                            var pair = current.frameA.time + ':' + current.frameB.time;
                            if (pair !== skippedPair && (!isLoaded(current.frameA, keys) || !isLoaded(current.frameB, keys))) {
                                var now = performance.now();
                                if (pair !== waitedPair) {
                                    waitedPair = pair;
                                    waitStart = now;
                                }
                                if (now - waitStart < $MAX_BUFFER_HOLD_MS) {
                                    this.lastTickTime = now;
                                    return;
                                }
                                skippedPair = pair;
                            }
                        }
                    } catch (e) {
                        console.error('MapTiler Weather: buffered playback disabled', e);
                        l.animationTick = originalTick;
                    }
                    return originalTick.apply(this, arguments);
                };
            })($layerVar);
            """

        /**
         * JS expression resolving the default insertion point: above water, beneath borders and labels.
         */
        internal const val BEFORE_ID_JS_SNIPPET = """
        (function() {
            var styleObj = ${MTBridge.MAP_OBJECT}.getStyle();
            var layers = styleObj ? styleObj.layers : [];
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
