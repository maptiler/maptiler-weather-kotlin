/*
 * Copyright (c) 2026, MapTiler
 * All rights reserved.
 * SPDX-License-Identifier: BSD 3-Clause
 */

package com.maptiler.maptilerweather.commands

import com.maptiler.maptilersdk.bridge.MTCommand

/**
 * Returns whether the weather module has been injected into the currently loaded map page.
 */
internal object IsWeatherModuleLoaded : MTCommand {
    override val isPrimitiveReturnType: Boolean = true

    override fun toJS(): String = "!!window.maptilerweather;"
}
