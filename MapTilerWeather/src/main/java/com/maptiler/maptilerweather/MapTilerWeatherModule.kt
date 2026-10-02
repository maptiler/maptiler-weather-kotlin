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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

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

        try {
            val json = JsonConfig.json.decodeFromString<JsonObject>(data)
            val layerId = json["layerId"]?.jsonPrimitive?.contentOrNull ?: return

            val targetLayers = MTWeatherLayer.activeLayers.filter { it.identifier == layerId }

            val dataMap =
                json.mapValues { (_, value) ->
                    value.jsonPrimitive.doubleOrNull ?: value.jsonPrimitive.contentOrNull
                }

            for (layer in targetLayers) {
                layer.handleEvent(event, dataMap)
            }
        } catch (e: Exception) {
            Log.e("onMessageReceived", "$e")
        }
    }
}
