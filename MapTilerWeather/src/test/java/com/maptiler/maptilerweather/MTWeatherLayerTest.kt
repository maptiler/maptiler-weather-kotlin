/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import android.content.Context
import com.maptiler.maptilersdk.map.MTMapViewController
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MTWeatherLayerTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPressureLayerPickAt() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTPressureLayer(identifier = "test-pressure-layer")
            layer.controller = mockController

            val commandSlot = slot<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(commandSlot)) } } coAnswers {
                val js = commandSlot.captured.toJS()
                val requestIdPattern = """requestId: "([^"]+)"""".toRegex()
                val match = requestIdPattern.find(js)
                val requestId = match?.groupValues?.get(1)

                assertNotNull(requestId)

                val responseJson =
                    buildJsonObject {
                        put("layerId", "test-pressure-layer")
                        put("requestId", requestId!!)
                        put(
                            "value",
                            buildJsonObject {
                                put("value", 1013.25)
                            },
                        )
                    }

                layer.handlePickAtResult(responseJson)
                mockk()
            }

            val result = layer.pickAt(12.34, 56.78)
            assertNotNull(result)
            assertEquals(1013.25, result!!.value, 0.0)
        }

    @Test
    fun testTemperatureLayerPickAt() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTTemperatureLayer(identifier = "test-temp-layer")
            layer.controller = mockController

            val commandSlot = slot<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(commandSlot)) } } coAnswers {
                val js = commandSlot.captured.toJS()
                val requestId = """requestId: "([^"]+)"""".toRegex().find(js)?.groupValues?.get(1)
                assertNotNull(requestId)

                val responseJson =
                    buildJsonObject {
                        put("layerId", "test-temp-layer")
                        put("requestId", requestId!!)
                        put(
                            "value",
                            buildJsonObject {
                                put("value", 21.5)
                                put("valueImperial", 70.7)
                            },
                        )
                    }

                layer.handlePickAtResult(responseJson)
                mockk()
            }

            val result = layer.pickAt(12.34, 56.78)
            assertNotNull(result)
            assertEquals(21.5, result!!.value, 0.0)
            assertEquals(70.7, result.valueImperial, 0.0)
        }

    @Test
    fun testWindLayerPickAt() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTWindLayer(identifier = "test-wind-layer")
            layer.controller = mockController

            val commandSlot = slot<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(commandSlot)) } } coAnswers {
                val js = commandSlot.captured.toJS()
                val requestId = """requestId: "([^"]+)"""".toRegex().find(js)?.groupValues?.get(1)
                assertNotNull(requestId)

                val responseJson =
                    buildJsonObject {
                        put("layerId", "test-wind-layer")
                        put("requestId", requestId!!)
                        put(
                            "value",
                            buildJsonObject {
                                put("compassDirection", "NNE")
                                put("directionAngle", 22.5)
                                put("speedFeetPerSecond", 15.0)
                                put("speedKilometersPerHour", 16.5)
                                put("speedKnots", 8.9)
                                put("speedMetersPerSecond", 4.5)
                                put("speedMilesPerHour", 10.2)
                            },
                        )
                    }

                layer.handlePickAtResult(responseJson)
                mockk()
            }

            val result = layer.pickAt(12.34, 56.78)
            assertNotNull(result)
            assertEquals("NNE", result!!.compassDirection)
            assertEquals(22.5, result.directionAngle, 0.0)
            assertEquals(15.0, result.speedFeetPerSecond, 0.0)
            assertEquals(16.5, result.speedKilometersPerHour, 0.0)
            assertEquals(8.9, result.speedKnots, 0.0)
            assertEquals(4.5, result.speedMetersPerSecond, 0.0)
            assertEquals(10.2, result.speedMilesPerHour, 0.0)
        }

    @Test
    fun testPrecipitationLayerPickAt() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTPrecipitationLayer(identifier = "test-precip-layer")
            layer.controller = mockController

            val commandSlot = slot<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(commandSlot)) } } coAnswers {
                val js = commandSlot.captured.toJS()
                val requestId = """requestId: "([^"]+)"""".toRegex().find(js)?.groupValues?.get(1)
                assertNotNull(requestId)

                val responseJson =
                    buildJsonObject {
                        put("layerId", "test-precip-layer")
                        put("requestId", requestId!!)
                        put(
                            "value",
                            buildJsonObject {
                                put("value", 2.4)
                            },
                        )
                    }

                layer.handlePickAtResult(responseJson)
                mockk()
            }

            val result = layer.pickAt(12.34, 56.78)
            assertNotNull(result)
            assertEquals(2.4, result!!.value, 0.0)
        }

    @Test
    fun testRadarLayerPickAt() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTRadarLayer(identifier = "test-radar-layer")
            layer.controller = mockController

            val commandSlot = slot<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(commandSlot)) } } coAnswers {
                val js = commandSlot.captured.toJS()
                val requestId = """requestId: "([^"]+)"""".toRegex().find(js)?.groupValues?.get(1)
                assertNotNull(requestId)

                val responseJson =
                    buildJsonObject {
                        put("layerId", "test-radar-layer")
                        put("requestId", requestId!!)
                        put(
                            "value",
                            buildJsonObject {
                                put("value", 35.0)
                            },
                        )
                    }

                layer.handlePickAtResult(responseJson)
                mockk()
            }

            val result = layer.pickAt(12.34, 56.78)
            assertNotNull(result)
            assertEquals(35.0, result!!.value, 0.0)
        }

    @Test
    fun testPickAtNullValue() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTPressureLayer(identifier = "test-pressure-layer")
            layer.controller = mockController

            val commandSlot = slot<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(commandSlot)) } } coAnswers {
                val js = commandSlot.captured.toJS()
                val requestId = """requestId: "([^"]+)"""".toRegex().find(js)?.groupValues?.get(1)
                assertNotNull(requestId)

                val responseJson =
                    buildJsonObject {
                        put("layerId", "test-pressure-layer")
                        put("requestId", requestId!!)
                    }

                layer.handlePickAtResult(responseJson)
                mockk()
            }

            val result = layer.pickAt(12.34, 56.78)
            assertNull(result)
        }

    @Test
    fun testWeatherModuleOnMessageReceived_routesToSourceReady() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTPressureLayer(identifier = "test-layer")
            var onSourceReadyCalled = false
            layer.onSourceReady = {
                onSourceReadyCalled = true
            }

            val module = MapTilerWeatherModule(mockContext)
            module.onAttach(mockController)

            val eventPayload =
                """
                {
                    "layerId": "test-layer"
                }
                """.trimIndent()

            module.onMessageReceived("sourceReady", eventPayload)

            assertTrue("onSourceReady should be triggered on the layer", onSourceReadyCalled)
        }

    @Test
    fun testWeatherModuleOnMessageReceived_routesToPlayAnimation() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTPressureLayer(identifier = "test-layer-anim")
            var playAnimationTime: Double? = null
            layer.onPlayAnimation = { time ->
                playAnimationTime = time
            }

            val module = MapTilerWeatherModule(mockContext)
            module.onAttach(mockController)

            val eventPayload =
                """
                {
                    "layerId": "test-layer-anim",
                    "time": 123.45
                }
                """.trimIndent()

            module.onMessageReceived("playAnimation", eventPayload)

            assertEquals(123.45, playAnimationTime ?: 0.0, 0.0)
        }

    @Test
    fun testTileLayerAddAndRemoveSource() =
        runBlocking {
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val tileLayer = MTTileLayer(identifier = "test-tile-layer")
            tileLayer.controller = mockController

            val capturedCommands = mutableListOf<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(capturedCommands)) } } returns mockk()

            val testDate = java.util.Date(1700000000000L) // 1700000000 seconds
            tileLayer.addSource(testDate, "https://example.com/tiles/{z}/{x}/{y}.png")
            tileLayer.removeSource(testDate)

            assertEquals(2, capturedCommands.size)

            val jsAdd = capturedCommands[0].toJS()
            assertTrue("Should contain layer.addSource", jsAdd.contains("layer.addSource("))
            assertTrue("Should contain the test time", jsAdd.contains("1.7E9") || jsAdd.contains("1700000000"))
            assertTrue("Should contain the tile url", jsAdd.contains("https://example.com/tiles/"))

            val jsRemove = capturedCommands[1].toJS()
            assertTrue("Should contain layer.removeSource", jsRemove.contains("layer.removeSource("))
            assertTrue("Should contain the test time", jsRemove.contains("1.7E9") || jsRemove.contains("1700000000"))
        }

    @Test
    fun testIntensityLayerPickAt() =
        runBlocking {
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTIntensityLayer(identifier = "test-intensity-layer")
            layer.controller = mockController

            val commandSlot = slot<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(commandSlot)) } } coAnswers {
                val js = commandSlot.captured.toJS()
                val requestId = """requestId: "([^"]+)"""".toRegex().find(js)?.groupValues?.get(1)
                assertNotNull(requestId)

                val responseJson =
                    buildJsonObject {
                        put("layerId", "test-intensity-layer")
                        put("requestId", requestId!!)
                        put(
                            "value",
                            buildJsonObject {
                                put("value", 85.3)
                            },
                        )
                    }

                layer.handlePickAtResult(responseJson)
                mockk()
            }

            val result = layer.pickAt(12.34, 56.78) as? MTIntensityValue
            assertNotNull(result)
            assertEquals(85.3, result!!.value, 0.0)
        }
}
