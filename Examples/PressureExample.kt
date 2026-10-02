package com.maptiler.examples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maptiler.maptilersdk.map.LngLat
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapView
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilerweather.MTPressureLayer
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset

@Composable
fun PressureExample() {
    val context = LocalContext.current
    val controller = remember { MTMapViewController(context) }
    
    val options =
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 3.0,
        )
        
    MTMapView(
        modifier = Modifier.fillMaxSize(),
        referenceStyle = MTMapReferenceStyle.DATAVIZ,
        options = options,
        controller = controller,
    )

    val layer = MTPressureLayer()
        .opacity(0.8)
        .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE))
        
    layer.addTo(controller)
}
