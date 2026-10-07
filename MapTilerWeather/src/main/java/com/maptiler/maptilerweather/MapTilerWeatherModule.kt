/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import android.content.Context
import android.util.Base64
import android.util.Log
import com.maptiler.maptilersdk.bridge.MTCommand
import com.maptiler.maptilersdk.helpers.JsonConfig
import com.maptiler.maptilersdk.map.MTMapModule
import com.maptiler.maptilersdk.map.MTMapViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * The MapTiler Weather module. Register this with your [MTMapViewController] to enable weather functionality.
 */
class MapTilerWeatherModule(private val context: Context) : MTMapModule {
    override val id: String = "maptiler-weather"
    private var controller: MTMapViewController? = null

    override fun onAttach(controller: MTMapViewController) {
        this.controller = controller
    }

    override fun onMapReady() {
        Log.d("MapTilerWeatherModule", "onMapReady called")
        val jsContent =
            try {
                context.assets.open("maptiler-weather.js").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                return
            }

        val base64Js = Base64.encodeToString(jsContent.toByteArray(), Base64.NO_WRAP)

        val injectCommand =
            object : MTCommand {
                override val isPrimitiveReturnType: Boolean = true

                override fun toJS(): String =
                    """
                    (function() {
                        try {
                            var binary = window.atob('$base64Js');
                            var bytes = new Uint8Array(binary.length);
                            for (var i = 0; i < binary.length; i++) {
                                bytes[i] = binary.charCodeAt(i);
                            }
                            var text = new TextDecoder('utf-8').decode(bytes);
                            var script = document.createElement('script');
                            script.type = 'text/javascript';
                            script.text = text;
                            document.head.appendChild(script);
                        } catch (e) {
                            console.error("MapTilerWeatherModule: Failed to inject - " + e);
                        }
                    })();
                    """.trimIndent()
            }

        controller?.scope?.launch {
            controller?.execute(injectCommand)
        }
    }

    override fun onMessageReceived(
        event: String,
        data: String?,
    ) {
        if (data == null) return
        val controller = this.controller ?: return

        controller.scope?.launch(Dispatchers.Main) {
            try {
                val json = JsonConfig.json.decodeFromString<JsonObject>(data)
                val layerId = json["layerId"]?.jsonPrimitive?.contentOrNull ?: return@launch

                val targetLayers =
                    synchronized(MTWeatherLayer.activeLayers) {
                        MTWeatherLayer.activeLayers.filter { it.identifier == layerId }
                    }

                if (event == "pickAtResult") {
                    for (layer in targetLayers) {
                        layer.handlePickAtResult(json)
                    }
                    return@launch
                }

                val dataMap = json.mapValues { (_, value) -> value.toAny() }

                for (layer in targetLayers) {
                    layer.handleEvent(event, dataMap)
                }
            } catch (e: Exception) {
                Log.e("onMessageReceived", "$e")
            }
        }
    }
}

/**
 * Recursively converts a [JsonElement] into its native Kotlin representation
 * (e.g., Boolean, Long, Double, String, List, Map, or null).
 */
private fun JsonElement.toAny(): Any? {
    return when (this) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (isString) {
                content
            } else {
                booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
            }
        }
        is JsonArray -> map { it.toAny() }
        is JsonObject -> mapValues { it.value.toAny() }
    }
}
