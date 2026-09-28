/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import android.util.Log
import com.maptiler.maptilersdk.helpers.JsonConfig
import com.maptiler.maptilersdk.map.MTMapModule
import com.maptiler.maptilersdk.map.MTMapViewController
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The MapTiler Weather module. Register this with your [MTMapViewController] to enable weather functionality.
 */
class MapTilerWeatherModule : MTMapModule {
    override val id: String = "maptiler-weather"
    private var controller: MTMapViewController? = null

    override fun onAttach(controller: MTMapViewController) {
        this.controller = controller
    }

    override fun onMapReady() {
        // Injection of JS/CSS bundle would happen here in a real implementation
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
