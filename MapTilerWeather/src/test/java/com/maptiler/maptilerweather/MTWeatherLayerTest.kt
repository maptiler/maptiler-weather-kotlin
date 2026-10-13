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

    @Test
    fun testParticleLayerModifiersAndSerialization() {
        val layer =
            MTParticleLayer("test-particle")
                .color(MTRGBAColor(10, 20, 30, 40))
                .fastColor(MTRGBAColor(100, 110, 120, 130))
                .density(5.5)
                .fadeFactor(0.2)
                .fastSpeed(15.0)
                .maxAmount(256)
                .pixelRatio(2.0)
                .refreshInterval(500.0)
                .size(2.5)
                .speed(0.005)
                .fastIsLarger(true)

        assertEquals(10, layer.color?.red)
        assertEquals(20, layer.color?.green)
        assertEquals(30, layer.color?.blue)
        assertEquals(40, layer.color?.alpha)
        assertEquals(100, layer.fastColor?.red)
        assertEquals(110, layer.fastColor?.green)
        assertEquals(120, layer.fastColor?.blue)
        assertEquals(130, layer.fastColor?.alpha)
        assertEquals(5.5, layer.density ?: 0.0, 0.0)
        assertEquals(0.2, layer.fadeFactor ?: 0.0, 0.0)
        assertEquals(15.0, layer.fastSpeed ?: 0.0, 0.0)
        assertEquals(256, layer.maxAmount ?: 0)
        assertEquals(2.0, layer.pixelRatio ?: 0.0, 0.0)
        assertEquals(500.0, layer.refreshInterval ?: 0.0, 0.0)
        assertEquals(2.5, layer.size ?: 0.0, 0.0)
        assertEquals(0.005, layer.speed ?: 0.0, 0.0)
        assertEquals(true, layer.fastIsLarger)

        val jsonStr =
            com.maptiler.maptilersdk.helpers.JsonConfig.json.encodeToString(
                MTParticleLayer.serializer(),
                layer,
            )
        assertTrue(jsonStr.contains("\"id\":\"test-particle\""))
        assertTrue(jsonStr.contains("\"color\":[10,20,30,40]"))
        assertTrue(jsonStr.contains("\"fastColor\":[100,110,120,130]"))
        assertTrue(jsonStr.contains("\"density\":5.5"))
        assertTrue(jsonStr.contains("\"fadeFactor\":0.2"))
        assertTrue(jsonStr.contains("\"fastSpeed\":15.0"))
        assertTrue(jsonStr.contains("\"maxAmount\":256"))
        assertTrue(jsonStr.contains("\"pixelRatio\":2.0"))
        assertTrue(jsonStr.contains("\"refreshInterval\":500.0"))
        assertTrue(jsonStr.contains("\"size\":2.5"))
        assertTrue(jsonStr.contains("\"speed\":0.005"))
        assertTrue(jsonStr.contains("\"fastIsLarger\":true"))
    }

    @Test
    fun testColoringFragmentSerialization() {
        val decode = MTDecoderOptions("rg", -10.0, 45.0)
        val stops =
            listOf(
                com.maptiler.maptilerweather.colorramp.MTWeatherColorRampStop(0.0, listOf(0, 0, 0, 0)),
                com.maptiler.maptilerweather.colorramp.MTWeatherColorRampStop(1.0, listOf(255, 255, 255, 255)),
            )
        val fragment: MTColoringFragment =
            MTGradientColoringFragment(
                decode = decode,
                stops = stops,
                smooth = false,
                opacity = 0.8,
            )

        val jsonStr =
            com.maptiler.maptilersdk.helpers.JsonConfig.json.encodeToString(
                MTColoringFragment.serializer(),
                fragment,
            )
        assertTrue(jsonStr.contains("\"type\":\"gradient\""))
        assertTrue(jsonStr.contains("\"channel\":\"rg\""))
        assertTrue(jsonStr.contains("\"min\":-10.0"))
        assertTrue(jsonStr.contains("\"max\":45.0"))
        assertTrue(jsonStr.contains("\"smooth\":false"))
        assertTrue(jsonStr.contains("\"opacity\":0.8"))

        val deserialized = com.maptiler.maptilersdk.helpers.JsonConfig.json.decodeFromString<MTColoringFragment>(jsonStr)
        assertTrue(deserialized is MTGradientColoringFragment)
        val gradient = deserialized as MTGradientColoringFragment
        assertEquals("rg", gradient.decode.channel)
        assertEquals(-10.0, gradient.decode.min, 0.0)
        assertEquals(45.0, gradient.decode.max, 0.0)
        assertEquals(2, gradient.stops.size)
        assertEquals(0.0, gradient.stops[0].value, 0.0)
        assertEquals(255, gradient.stops[1].color[0])
    }

    @Test
    fun testWeatherLayerWithColoring() {
        val layer = MTTemperatureLayer(identifier = "temp-coloring-test")
        val decode = MTDecoderOptions("r", 0.0, 100.0)
        val stops =
            listOf(
                com.maptiler.maptilerweather.colorramp.MTWeatherColorRampStop(0.0, listOf(0, 0, 0, 255)),
            )
        val coloringFragment = MTGradientColoringFragment(decode, stops)
        layer.coloring(coloringFragment)

        val jsonStr =
            com.maptiler.maptilersdk.helpers.JsonConfig.json.encodeToString(
                MTWeatherLayer.serializer(),
                layer,
            )
        assertTrue(jsonStr.contains("\"coloring\":{\"decode\":{\"channel\":\"r\",\"min\":0.0,\"max\":100.0}"))
    }

    @Test
    fun testWeatherLayerAnimationMethods() =
        runBlocking {
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTTemperatureLayer(identifier = "test-anim-methods")
            layer.controller = mockController

            val capturedCommands = mutableListOf<com.maptiler.maptilersdk.bridge.MTCommand>()
            every { runBlocking { mockController.execute(capture(capturedCommands)) } } returns mockk()

            layer.setOpacity(0.75)
            layer.setAnimationTime(1700000000.0)
            layer.setAnimationTime(java.util.Date(1700000000000L))
            layer.animate(3600.0)
            layer.animateByFactor(2.0)
            layer.setRepaintOnPausedAnimation(false)
            layer.setTimeInterpolation(false)
            layer.setLocalSmoothing(false)

            assertEquals(8, capturedCommands.size)

            val jsSetOpacity = capturedCommands[0].toJS()
            assertTrue(jsSetOpacity.contains("layer.setOpacity(0.75)"))

            val jsSetAnimTime = capturedCommands[1].toJS()
            assertTrue(jsSetAnimTime.contains("layer.setAnimationTime("))

            val jsAnimate = capturedCommands[3].toJS()
            assertTrue(jsAnimate.contains("layer.animate(3600.0)"))

            val jsAnimateByFactor = capturedCommands[4].toJS()
            assertTrue(jsAnimateByFactor.contains("layer.animateByFactor(2.0)"))

            val jsRepaint = capturedCommands[5].toJS()
            assertTrue(jsRepaint.contains("layer.setRepaintOnPausedAnimation(false)"))

            val jsInterpolation = capturedCommands[6].toJS()
            assertTrue(jsInterpolation.contains("layer.setTimeInterpolation(false)"))

            val jsSmoothing = capturedCommands[7].toJS()
            assertTrue(jsSmoothing.contains("layer.setLocalSmoothing(false)"))
        }

    @Test
    fun testWeatherLayerGetters() =
        runBlocking {
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTTemperatureLayer(identifier = "test-getters")
            layer.controller = mockController

            val capturedCommands = mutableListOf<com.maptiler.maptilersdk.bridge.MTCommand>()
            every {
                runBlocking {
                    mockController.execute(capture(capturedCommands))
                }
            } returns com.maptiler.maptilersdk.bridge.MTBridgeReturnType.DoubleValue(1700000000.0)

            val animStart = layer.getAnimationStart()
            assertEquals(1700000000.0, animStart ?: 0.0, 0.0)

            val animStartDate = layer.getAnimationStartDate()
            assertNotNull(animStartDate)
            assertEquals(1700000000000L, animStartDate!!.time)

            every {
                runBlocking { mockController.execute(any()) }
            } returns com.maptiler.maptilersdk.bridge.MTBridgeReturnType.BoolValue(true)
            val isPlaying = layer.isPlaying()
            assertEquals(true, isPlaying)
        }

    @Test
    fun testWeatherLayerEvents() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTTemperatureLayer(identifier = "test-layer-events")
            var pauseTime: Double? = null
            var tickTime: Double? = null
            var animSetTime: Double? = null

            layer.onPauseAnimation = { pauseTime = it }
            layer.onTick = { tickTime = it }
            layer.onAnimationTimeSet = { animSetTime = it }

            val module = MapTilerWeatherModule(mockContext)
            module.onAttach(mockController)

            module.onMessageReceived("pauseAnimation", """{"layerId":"test-layer-events","time":100.0}""")
            module.onMessageReceived("tick", """{"layerId":"test-layer-events","time":150.0}""")
            module.onMessageReceived("animationTimeSet", """{"layerId":"test-layer-events","time":200.0}""")

            assertEquals(100.0, pauseTime ?: 0.0, 0.0)
            assertEquals(150.0, tickTime ?: 0.0, 0.0)
            assertEquals(200.0, animSetTime ?: 0.0, 0.0)
        }

    @Test
    fun testWeatherLayerDSLModifiers() {
        val testDate = java.util.Date(1700000000000L)
        val layer =
            MTTemperatureLayer("test-dsl")
                .repaintOnPausedAnimation(false)
                .timeInterpolation(false)
                .localSmoothing(false)
                .nbSmoothingBins(12)
                .maxSmoothingDistance(50.0)
                .smoothingDistanceDecayFactor(0.5)
                .loadLowerZoomLevels(true)
                .renderTransparentArea(true)
                .visibility(com.maptiler.maptilersdk.map.style.layer.MTLayerVisibility.NONE)
                .time(testDate)
                .maxZoom(10.0)
                .minZoom(2.0)
                .beforeId("water")

        assertEquals(false, layer.repaintOnPausedAnimation)
        assertEquals(false, layer.timeInterpolation)
        assertEquals(false, layer.localSmoothing)
        assertEquals(12, layer.nbSmoothingBins)
        assertEquals(50.0, layer.maxSmoothingDistance ?: 0.0, 0.0)
        assertEquals(0.5, layer.smoothingDistanceDecayFactor ?: 0.0, 0.0)
        assertEquals(true, layer.loadLowerZoomLevels)
        assertEquals(true, layer.renderTransparentArea)
        assertEquals(com.maptiler.maptilersdk.map.style.layer.MTLayerVisibility.NONE, layer.visibility)
        assertEquals(1700000000000L, layer.time)
        assertEquals(10.0, layer.maxZoom ?: 0.0, 0.0)
        assertEquals(2.0, layer.minZoom ?: 0.0, 0.0)
        assertEquals("water", layer.beforeId)
    }

    @Test
    fun testWindLayerInheritsFromParticleLayer() {
        val windLayer = MTWindLayer("wind-particle-test")
        val particleLayer: MTParticleLayer = windLayer

        particleLayer.density(3.0).fastIsLarger(true).speed(0.002)
        assertEquals(3.0, windLayer.density ?: 0.0, 0.0)
        assertEquals(true, windLayer.fastIsLarger)
        assertEquals(0.002, windLayer.speed ?: 0.0, 0.0)
    }
}
