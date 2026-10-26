package com.maptiler.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maptiler.maptilersdk.map.LngLat
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapView
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.options.MTEventLevel
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilerweather.MTPrecipitationLayer
import com.maptiler.maptilerweather.MTRadarLayer
import com.maptiler.maptilerweather.MTWeatherLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule

private enum class PerformanceLayer(val title: String) {
    PRECIPITATION("Precipitation"),
    RADAR("Radar"),
}

/**
 * Example with only a map, one weather layer and its animation, for checking playback smoothness.
 */
@Composable
fun TimeAnimationExample() {
    val context = LocalContext.current
    val controller = remember {
        MTMapViewController(context).apply {
            registerModule(MapTilerWeatherModule(context))
        }
    }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 3.0,
            eventLevel = MTEventLevel.ESSENTIAL,
        )
    }

    var selected by remember { mutableStateOf(PerformanceLayer.PRECIPITATION) }
    var layer by remember { mutableStateOf<MTWeatherLayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        MTMapView(
            modifier = Modifier.fillMaxSize(),
            referenceStyle = MTMapReferenceStyle.DATAVIZ,
            options = options,
            controller = controller,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            PerformanceLayer.values().forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { selected = option },
                    label = { Text(option.title) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Color.White)
                )
            }

            Button(
                onClick = {
                    isPlaying = !isPlaying
                    layer?.animateByFactor(if (isPlaying) 3600.0 else 0.0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088FF))
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
    }

    LaunchedEffect(selected) {
        layer?.removeFrom(controller)
        isPlaying = false

        val newLayer = when (selected) {
            PerformanceLayer.PRECIPITATION -> MTPrecipitationLayer()
            PerformanceLayer.RADAR -> MTRadarLayer()
        }
        newLayer.addTo(controller)
        layer = newLayer
    }
}
