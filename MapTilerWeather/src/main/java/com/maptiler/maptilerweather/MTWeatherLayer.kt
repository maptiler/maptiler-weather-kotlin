/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import com.maptiler.maptilersdk.bridge.MTBridgeReturnType
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.style.layer.MTLayer
import com.maptiler.maptilersdk.map.style.layer.MTLayerType
import com.maptiler.maptilersdk.map.style.layer.MTLayerVisibility
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.commands.AddWeatherLayer
import com.maptiler.maptilerweather.commands.RemoveWeatherLayer
import com.maptiler.maptilerweather.commands.WeatherCommandArgument
import com.maptiler.maptilerweather.commands.WeatherLayerMethodCommand
import com.maptiler.maptilerweather.commands.WeatherLayerValueCommand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Collections
import java.util.Date
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
     * Coloring fragment used to represent the weather data.
     * This is a more advanced way to specify coloring, including the data decoder.
     */
    @SerialName("coloring")
    var coloring: MTColoringFragment? = null

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
     * Called during animation, at most every 50 ms (20 times per second). Provides the timestamp in seconds.
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

    // MARK: - Animation & Methods

    /**
     * Sets the opacity at which the weather data will be drawn.
     *
     * @param opacity Opacity value between 0.0 and 1.0.
     */
    fun setOpacity(opacity: Double) {
        this.opacity = opacity
        executeMethod("setOpacity", listOf(WeatherCommandArgument.DoubleArg(opacity)))
    }

    /**
     * Sets the animation time in seconds.
     *
     * @param time Timestamp in seconds.
     */
    fun setAnimationTime(time: Double) {
        this.time = (time * 1000).toLong()
        executeMethod("setAnimationTime", listOf(WeatherCommandArgument.DoubleArg(time)))
    }

    /**
     * Sets the animation time using a [Date].
     *
     * @param date Date object.
     */
    fun setAnimationTime(date: Date) {
        setAnimationTime(date.time / 1000.0)
    }

    /**
     * Animates the layer at a speed of [timePerSecond] seconds of weather animation per real-world second.
     * Pass 0 to pause animation.
     *
     * @param timePerSecond Seconds of animation per second.
     */
    fun animate(timePerSecond: Double) {
        executeMethod("animate", listOf(WeatherCommandArgument.DoubleArg(timePerSecond)))
    }

    /**
     * Animates the layer multiplied by a speed factor.
     *
     * @param factor Speed factor.
     */
    fun animateByFactor(factor: Double) {
        executeMethod("animateByFactor", listOf(WeatherCommandArgument.DoubleArg(factor)))
    }

    /**
     * Gets the start time of the animation sequence in seconds.
     *
     * @return Start timestamp in seconds, or null if unavailable.
     */
    suspend fun getAnimationStart(): Double? = getDoubleValue("getAnimationStart")

    /**
     * Gets the start date of the animation sequence.
     *
     * @return Start date, or null if unavailable.
     */
    suspend fun getAnimationStartDate(): Date? = getDateValue("getAnimationStart")

    /**
     * Gets the end time of the animation sequence in seconds.
     *
     * @return End timestamp in seconds, or null if unavailable.
     */
    suspend fun getAnimationEnd(): Double? = getDoubleValue("getAnimationEnd")

    /**
     * Gets the end date of the animation sequence.
     *
     * @return End date, or null if unavailable.
     */
    suspend fun getAnimationEndDate(): Date? = getDateValue("getAnimationEnd")

    /**
     * Gets the current animation time in seconds.
     *
     * @return Current animation time in seconds, or null if unavailable.
     */
    suspend fun getAnimationTime(): Double? = getDoubleValue("getAnimationTime")

    /**
     * Gets the current animation date.
     *
     * @return Current animation date, or null if unavailable.
     */
    suspend fun getAnimationTimeDate(): Date? = getDateValue("getAnimationTime")

    /**
     * Gets the current animation speed factor.
     *
     * @return Speed factor, or null if unavailable.
     */
    suspend fun getAnimationSpeed(): Double? = getDoubleValue("getAnimationSpeed")

    /**
     * Sets whether repaint is enabled when animation is paused.
     *
     * @param enabled True to enable repaint on paused animation.
     */
    fun setRepaintOnPausedAnimation(enabled: Boolean) {
        this.repaintOnPausedAnimation = enabled
        executeMethod("setRepaintOnPausedAnimation", listOf(WeatherCommandArgument.BoolArg(enabled)))
    }

    /**
     * Sets whether time interpolation is enabled.
     *
     * @param enabled True to enable time interpolation.
     */
    fun setTimeInterpolation(enabled: Boolean) {
        this.timeInterpolation = enabled
        executeMethod("setTimeInterpolation", listOf(WeatherCommandArgument.BoolArg(enabled)))
    }

    /**
     * Sets whether local smoothing is enabled.
     *
     * @param enabled True to enable local smoothing.
     */
    fun setLocalSmoothing(enabled: Boolean) {
        this.localSmoothing = enabled
        executeMethod("setLocalSmoothing", listOf(WeatherCommandArgument.BoolArg(enabled)))
    }

    /**
     * Checks whether the layer animation is currently playing.
     *
     * @return True if animation is playing, false if paused, or null if unavailable.
     */
    suspend fun isPlaying(): Boolean? = getBoolValue("isPlaying")

    private fun executeMethod(
        methodName: String,
        arguments: List<WeatherCommandArgument>,
    ) {
        val controller = this.controller ?: return
        controller.scope?.launch {
            val command = WeatherLayerMethodCommand(identifier, methodName, arguments)
            controller.execute(command)
        }
    }

    private suspend fun getDoubleValue(methodName: String): Double? {
        val controller = this.controller ?: return null
        val command = WeatherLayerValueCommand(identifier, methodName)
        val result = controller.execute(command)
        return when (result) {
            is MTBridgeReturnType.DoubleValue -> result.value
            is MTBridgeReturnType.StringValue -> result.value.trim('"').toDoubleOrNull()
            else -> null
        }
    }

    private suspend fun getDateValue(methodName: String): Date? {
        val timestamp = getDoubleValue(methodName) ?: return null
        if (timestamp.isInfinite() || timestamp.isNaN()) return null
        return Date((timestamp * 1000).toLong())
    }

    private suspend fun getBoolValue(methodName: String): Boolean? {
        val controller = this.controller ?: return null
        val command = WeatherLayerValueCommand(identifier, methodName)
        val result = controller.execute(command)
        return when (result) {
            is MTBridgeReturnType.BoolValue -> result.value
            is MTBridgeReturnType.DoubleValue -> result.value != 0.0
            is MTBridgeReturnType.StringValue -> {
                val str = result.value.trim('"').lowercase()
                str.toBooleanStrictOrNull() ?: (str.toDoubleOrNull()?.let { it != 0.0 })
            }
            else -> null
        }
    }

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
     * Modifier. Sets the repaintOnPausedAnimation property.
     */
    fun repaintOnPausedAnimation(value: Boolean): MTWeatherLayer {
        this.repaintOnPausedAnimation = value
        return this
    }

    /**
     * Modifier. Sets the timeInterpolation property.
     */
    fun timeInterpolation(value: Boolean): MTWeatherLayer {
        this.timeInterpolation = value
        return this
    }

    /**
     * Modifier. Sets the localSmoothing property.
     */
    fun localSmoothing(value: Boolean): MTWeatherLayer {
        this.localSmoothing = value
        return this
    }

    /**
     * Modifier. Sets the number of smoothing bins.
     */
    fun nbSmoothingBins(value: Int): MTWeatherLayer {
        this.nbSmoothingBins = value
        return this
    }

    /**
     * Modifier. Sets the maximum smoothing distance.
     */
    fun maxSmoothingDistance(value: Double): MTWeatherLayer {
        this.maxSmoothingDistance = value
        return this
    }

    /**
     * Modifier. Sets the smoothing distance decay factor.
     */
    fun smoothingDistanceDecayFactor(value: Double): MTWeatherLayer {
        this.smoothingDistanceDecayFactor = value
        return this
    }

    /**
     * Modifier. Sets loadLowerZoomLevels property.
     */
    fun loadLowerZoomLevels(value: Boolean): MTWeatherLayer {
        this.loadLowerZoomLevels = value
        return this
    }

    /**
     * Modifier. Sets renderTransparentArea property.
     */
    fun renderTransparentArea(value: Boolean): MTWeatherLayer {
        this.renderTransparentArea = value
        return this
    }

    /**
     * Modifier. Sets visibility.
     */
    fun visibility(value: MTLayerVisibility): MTWeatherLayer {
        this.visibility = value
        return this
    }

    /**
     * Modifier. Sets time in milliseconds.
     */
    fun time(value: Long): MTWeatherLayer {
        this.time = value
        return this
    }

    /**
     * Modifier. Sets time using a Date.
     */
    fun time(value: Date): MTWeatherLayer {
        this.time = value.time
        return this
    }

    /**
     * Modifier. Sets maxZoom.
     */
    fun maxZoom(value: Double): MTWeatherLayer {
        this.maxZoom = value
        return this
    }

    /**
     * Modifier. Sets minZoom.
     */
    fun minZoom(value: Double): MTWeatherLayer {
        this.minZoom = value
        return this
    }

    /**
     * Modifier. Sets beforeId.
     */
    fun beforeId(value: String?): MTWeatherLayer {
        this.beforeId = value
        return this
    }

    /**
     * Modifier. Sets the color ramp.
     */
    fun colorRamp(value: MTWeatherColorRamp): MTWeatherLayer {
        this.colorRamp = value
        return this
    }

    /**
     * Modifier. Sets the coloring fragment.
     */
    fun coloring(value: MTColoringFragment): MTWeatherLayer {
        this.coloring = value
        return this
    }
}
