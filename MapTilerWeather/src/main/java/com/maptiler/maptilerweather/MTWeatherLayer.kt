/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.style.layer.MTLayer
import com.maptiler.maptilersdk.map.style.layer.MTLayerType
import com.maptiler.maptilersdk.map.style.layer.MTLayerVisibility
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.commands.AddWeatherLayer
import com.maptiler.maptilerweather.commands.RemoveWeatherLayer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Collections
import java.util.WeakHashMap

/**
 * Base class for all MapTiler Weather layers.
 */
@Serializable
open class MTWeatherLayer(
    @SerialName("id")
    override var identifier: String,
    @SerialName("source")
    override var sourceIdentifier: String = "",
) : MTLayer {
    override val type: MTLayerType = MTLayerType.WEATHER

    @SerialName("maxzoom")
    override var maxZoom: Double? = null

    @SerialName("minzoom")
    override var minZoom: Double? = null

    @SerialName("source-layer")
    override var sourceLayer: String? = null

    /**
     * The opacity at which the weather data will be drawn.
     * Optional number between 0 and 1 inclusive. Defaults to 1.
     */
    var opacity: Double? = 1.0

    /**
     * Whether or not the colorramp must be smooth. Defaults to true.
     */
    var smooth: Boolean? = true

    /**
     * Determining if map.triggerRepaint() is called when the animation is paused.
     * Defaults to true.
     */
    @SerialName("repaintOnPausedAnimation")
    var repaintOnPausedAnimation: Boolean? = true

    /**
     * Whether or not to use time interpolation. Defaults to true.
     */
    @SerialName("timeInterpolation")
    var timeInterpolation: Boolean? = true

    /**
     * Whether or not to use local smoothing. Defaults to true.
     */
    @SerialName("localSmoothing")
    var localSmoothing: Boolean? = true

    /**
     * Number of smoothing bins.
     */
    @SerialName("nbSmoothingBins")
    var nbSmoothingBins: Int? = null

    /**
     * Maximum smoothing distance.
     */
    @SerialName("maxSmoothingDistance")
    var maxSmoothingDistance: Double? = null

    /**
     * Smoothing distance decay factor.
     */
    @SerialName("smoothingDistanceDecayFactor")
    var smoothingDistanceDecayFactor: Double? = null

    /**
     * Whether or not to load lower zoom levels.
     */
    @SerialName("loadLowerZoomLevels")
    var loadLowerZoomLevels: Boolean? = null

    /**
     * Whether or not to render transparent area.
     */
    @SerialName("renderTransparentArea")
    var renderTransparentArea: Boolean? = null

    /**
     * Whether this layer is displayed. Defaults to [MTLayerVisibility.VISIBLE].
     */
    var visibility: MTLayerVisibility? = MTLayerVisibility.VISIBLE

    /**
     * The ID of an existing layer to insert this weather layer before.
     * If null, the SDK will automatically attempt to place it below the first symbol (label) layer.
     */
    @Transient
    var beforeId: String? = null

    /**
     * Color ramp used to represent the weather data.
     */
    @SerialName("colorramp")
    var colorRamp: MTWeatherColorRamp? = null

    /**
     * Time for which to display the weather data.
     */
    var time: Long? = null

    /**
     * Weak reference to the map view controller this layer is added to.
     */
    @Transient
    internal var controller: MTMapViewController? = null

    // MARK: - Events

    /**
     * Called only once after the layer has been added to the map,
     * when all the necessary weather data source are loaded and ready to be used.
     *
     * **Thread Safety:** This callback is executed on the **Main (UI) thread**. Avoid performing
     * blocking or heavy operations here; offload long-running work to a background dispatcher.
     */
    @Transient
    var onSourceReady: (() -> Unit)? = null

    /**
     * Called when the animation is starting to play or plays after having been on pause
     * after calling `.animate(...)`. Provides the timestamp in seconds.
     *
     * **Thread Safety:** This callback is executed on the **Main (UI) thread**. Avoid performing
     * blocking or heavy operations here; offload long-running work to a background dispatcher.
     */
    @Transient
    var onPlayAnimation: ((Double) -> Unit)? = null

    /**
     * Called when the animation is being paused after calling `.animate(0)`. Provides the timestamp in seconds.
     *
     * **Thread Safety:** This callback is executed on the **Main (UI) thread**. Avoid performing
     * blocking or heavy operations here; offload long-running work to a background dispatcher.
     */
    @Transient
    var onPauseAnimation: ((Double) -> Unit)? = null

    /**
     * Called for each animation update, possibly many times per second. Provides the timestamp in seconds.
     *
     * **Thread Safety Warning:** This callback runs on the **Main (UI) thread** to allow safe and
     * synchronized UI updates. Because it fires at a high frequency, performing heavy computations or
     * blocking operations (e.g. database/network queries or complex data parsing) directly inside this callback
     * will block the main thread and cause the map animation or UI to stutter. Any heavy or long-running tasks
     * must be explicitly offloaded to a background dispatcher (such as `Dispatchers.IO` or `Dispatchers.Default`)
     * using coroutines.
     */
    @Transient
    var onTick: ((Double) -> Unit)? = null

    /**
     * Called when the progress time of the animation is manually set with
     * `.setAnimationTime(...)`. Provides the timestamp in seconds.
     *
     * **Thread Safety:** This callback is executed on the **Main (UI) thread**. Avoid performing
     * blocking or heavy operations here; offload long-running work to a background dispatcher.
     */
    @Transient
    var onAnimationTimeSet: ((Double) -> Unit)? = null

    @Transient
    private val pendingPickAtRequests = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<JsonObject?>>()

    /**
     * Picks weather data at a specific location.
     *
     * @param lng Longitude.
     * @param lat Latitude.
     * @return The weather data at the given location as a JSON object, or null if not available.
     */
    internal suspend fun pickAtRaw(
        lng: Double,
        lat: Double,
    ): JsonObject? {
        val controller = this.controller ?: return null
        val requestId = java.util.UUID.randomUUID().toString()
        val deferred = CompletableDeferred<JsonObject?>()
        pendingPickAtRequests[requestId] = deferred

        val weatherModuleString = "maptiler-weather"
        val pickAtResultString = "pickAtResult"

        controller.scope?.launch {
            val command =
                object : com.maptiler.maptilersdk.bridge.MTCommand {
                    override val isPrimitiveReturnType: Boolean = false

                    override fun toJS(): String =
                        """
                        (function() {
                            var styleLayer = map.getLayer("${this@MTWeatherLayer.identifier}");
                            var layer = styleLayer ? (styleLayer.implementation || styleLayer) : null;
                            if (layer && typeof layer.pickAt === 'function') {
                                try {
                                    var res = layer.pickAt($lng, $lat);
                                    if (window.Android && window.Android.onModuleEvent) {
                                        window.Android.onModuleEvent(
                                            "$weatherModuleString",
                                            "$pickAtResultString",
                                            JSON.stringify({
                                                layerId: "${this@MTWeatherLayer.identifier}",
                                                requestId: "$requestId",
                                                value: res
                                            })
                                        );
                                    }
                                } catch (err) {
                                    if (window.Android && window.Android.onModuleEvent) {
                                        window.Android.onModuleEvent(
                                            "$weatherModuleString",
                                            "$pickAtResultString",
                                            JSON.stringify({
                                                layerId: "${this@MTWeatherLayer.identifier}",
                                                requestId: "$requestId",
                                                error: err ? err.message : "unknown error"
                                            })
                                        );
                                    }
                                }
                            } else {
                                if (window.Android && window.Android.onModuleEvent) {
                                    window.Android.onModuleEvent(
                                        "$weatherModuleString",
                                        "$pickAtResultString",
                                        JSON.stringify({
                                            layerId: "${this@MTWeatherLayer.identifier}",
                                            requestId: "$requestId",
                                            error: "layer or pickAt function not found"
                                        })
                                    );
                                }
                            }
                        })();
                        """.trimIndent()
                }
            controller.execute(command)
        }

        return try {
            deferred.await()
        } finally {
            pendingPickAtRequests.remove(requestId)
        }
    }

    open suspend fun pickAt(
        lng: Double,
        lat: Double,
    ): Any? {
        return pickAtRaw(lng, lat)
    }

    internal fun handlePickAtResult(json: JsonObject) {
        val requestId = json["requestId"]?.jsonPrimitive?.contentOrNull ?: return
        val deferred = pendingPickAtRequests[requestId] ?: return

        val error = json["error"]?.jsonPrimitive?.contentOrNull
        if (error != null) {
            deferred.completeExceptionally(Exception(error))
        } else {
            val valueObj = json["value"] as? JsonObject
            deferred.complete(valueObj)
        }
    }

    companion object {
        /**
         * Active layers tracking for event routing.
         */
        internal val activeLayers: MutableSet<MTWeatherLayer> =
            Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))
    }

    init {
        activeLayers.add(this)
    }

    /**
     * The name of the class used to instantiate this layer.
     */
    open val jsClassName: String
        get() = "WeatherLayer"

    /**
     * Adds the weather layer to the map.
     *
     * @param map The map view controller to add the layer to.
     */
    fun addTo(map: MTMapViewController) {
        this.controller = map
        map.scope?.launch {
            val command = AddWeatherLayer(this@MTWeatherLayer, beforeId)
            map.execute(command)
        }
    }

    /**
     * Removes the weather layer from the map.
     *
     * @param map The map view controller to remove the layer from.
     */
    fun removeFrom(map: MTMapViewController) {
        map.scope?.launch {
            val command = RemoveWeatherLayer(identifier)
            map.execute(command)
        }
        activeLayers.remove(this)
        this.controller = null
    }

    internal fun handleEvent(
        event: String,
        data: Map<String, Any?>?,
    ) {
        when (event) {
            "sourceReady" -> onSourceReady?.invoke()
            "playAnimation" -> (data?.get("time") as? Double)?.let { onPlayAnimation?.invoke(it) }
            "pauseAnimation" -> (data?.get("time") as? Double)?.let { onPauseAnimation?.invoke(it) }
            "tick" -> (data?.get("time") as? Double)?.let { onTick?.invoke(it) }
            "animationTimeSet" -> (data?.get("time") as? Double)?.let { onAnimationTimeSet?.invoke(it) }
        }
    }

    // MARK: - DSL Modifiers

    /**
     * Modifier. Sets the opacity.
     */
    fun opacity(value: Double): MTWeatherLayer {
        this.opacity = value
        return this
    }

    /**
     * Modifier. Sets the smooth property.
     */
    fun smooth(value: Boolean): MTWeatherLayer {
        this.smooth = value
        return this
    }

    /**
     * Modifier. Sets the color ramp.
     */
    fun colorRamp(value: MTWeatherColorRamp): MTWeatherLayer {
        this.colorRamp = value
        return this
    }
}
