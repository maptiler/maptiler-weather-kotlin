/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather

import kotlinx.serialization.Serializable

/**
 * Options for decoding weather data from raster tiles.
 */
@Serializable
data class MTDecoderOptions(
    /**
     * The color channel(s) to read data from.
     * Common values are "r", "g", "b", "rg", etc.
     */
    var channel: String,
    /**
     * The real-world value that corresponds to a pixel value of 0.
     */
    var min: Double,
    /**
     * The real-world value that corresponds to a pixel value of 255.
     */
    var max: Double,
)
