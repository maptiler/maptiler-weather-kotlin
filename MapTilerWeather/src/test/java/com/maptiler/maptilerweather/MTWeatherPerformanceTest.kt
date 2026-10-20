/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import android.content.Context
import com.maptiler.maptilersdk.helpers.JsonConfig
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampStop
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.system.measureNanoTime

class MTWeatherPerformanceTest {
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
    fun benchmarkCommandAssembly() {
        val warmupRuns = 1000
        val testRuns = 10000

        // Warmup
        for (i in 0 until warmupRuns) {
            val layer =
                MTWindLayer("warmup-wind-$i")
                    .density(2.5)
                    .speed(0.005)
            val cmd = com.maptiler.maptilerweather.commands.AddWeatherLayer(layer)
            cmd.toJS()
        }

        // Measure Command Assembly for Wind Layer
        val totalNanosWind =
            measureNanoTime {
                for (i in 0 until testRuns) {
                    val layer =
                        MTWindLayer("wind-layer-$i")
                            .density(2.5)
                            .speed(0.005)
                    val cmd = com.maptiler.maptilerweather.commands.AddWeatherLayer(layer)
                    cmd.toJS()
                }
            }

        // Measure Command Assembly for Temperature Layer
        val totalNanosTemp =
            measureNanoTime {
                for (i in 0 until testRuns) {
                    val layer = MTTemperatureLayer("temp-layer-$i")
                    layer.opacity = 0.8
                    val cmd = com.maptiler.maptilerweather.commands.AddWeatherLayer(layer)
                    cmd.toJS()
                }
            }

        val avgMsWind = (totalNanosWind.toDouble() / testRuns) / 1_000_000.0
        val avgMsTemp = (totalNanosTemp.toDouble() / testRuns) / 1_000_000.0
        val avgMsOverall = (avgMsWind + avgMsTemp) / 2.0

        println("=== BENCHMARK: Command Assembly ===")
        println("Wind Layer Add Command: ${String.format("%.4f", avgMsWind)} ms per assembly")
        println("Temp Layer Add Command: ${String.format("%.4f", avgMsTemp)} ms per assembly")
        println("Average Command Assembly: ${String.format("%.4f", avgMsOverall)} ms")
    }

    @Test
    fun benchmarkConfigurationPrep() {
        val warmupRuns = 1000
        val testRuns = 10000

        // Warmup
        for (i in 0 until warmupRuns) {
            val particleLayer =
                MTParticleLayer("warmup-$i")
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

            JsonConfig.json.encodeToString(MTParticleLayer.serializer(), particleLayer)
        }

        val totalNanos =
            measureNanoTime {
                for (i in 0 until testRuns) {
                    val particleLayer =
                        MTParticleLayer("particle-$i")
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

                    JsonConfig.json.encodeToString(MTParticleLayer.serializer(), particleLayer)
                }
            }

        val avgMs = (totalNanos.toDouble() / testRuns) / 1_000_000.0

        println("=== BENCHMARK: Configuration Prep ===")
        println("Particle Layer Serialization: ${String.format("%.4f", avgMs)} ms")
    }

    @Test
    fun benchmarkColorProcessing() {
        val warmupRuns = 5000
        val testRuns = 50000

        // Warmup
        for (i in 0 until warmupRuns) {
            val color = MTRGBAColor(i % 256, (i * 2) % 256, (i * 3) % 256, 255)
            val stop = MTWeatherColorRampStop(i.toDouble(), listOf(color.red, color.green, color.blue, color.alpha))
            JsonConfig.json.encodeToString(MTWeatherColorRampStop.serializer(), stop)
        }

        val totalNanos =
            measureNanoTime {
                for (i in 0 until testRuns) {
                    val color = MTRGBAColor(i % 256, (i * 2) % 256, (i * 3) % 256, 255)
                    val stop = MTWeatherColorRampStop(i.toDouble(), listOf(color.red, color.green, color.blue, color.alpha))
                    JsonConfig.json.encodeToString(MTWeatherColorRampStop.serializer(), stop)
                }
            }

        val avgUs = (totalNanos.toDouble() / testRuns) / 1_000.0

        println("=== BENCHMARK: Color Processing ===")
        println("Color Lookup & Stop Prep: ${String.format("%.3f", avgUs)} microseconds")
    }

    @Test
    fun benchmarkProcessMessage() =
        runBlocking {
            val mockContext = mockk<Context>(relaxed = true)
            val mockController = mockk<MTMapViewController>(relaxed = true)
            val testScope = CoroutineScope(Dispatchers.Unconfined)
            every { mockController.scope } returns testScope

            val layer = MTTemperatureLayer(identifier = "bench-layer")
            val module = MapTilerWeatherModule(mockContext)
            module.onAttach(mockController)

            val tickPayload = """{"layerId":"bench-layer","time":1700000000.0}"""

            val warmupRuns = 1000
            val testRuns = 10000

            // Warmup
            for (i in 0 until warmupRuns) {
                module.onMessageReceived("tick", tickPayload)
            }

            val totalNanos =
                measureNanoTime {
                    for (i in 0 until testRuns) {
                        module.onMessageReceived("tick", tickPayload)
                    }
                }

            val avgMs = (totalNanos.toDouble() / testRuns) / 1_000_000.0
            val avgUs = (totalNanos.toDouble() / testRuns) / 1_000.0

            println("=== BENCHMARK: Process Message (JS -> Kotlin) ===")
            println("Average Message Handling: ${String.format("%.4f", avgMs)} ms (${String.format("%.2f", avgUs)} µs)")
        }
}
