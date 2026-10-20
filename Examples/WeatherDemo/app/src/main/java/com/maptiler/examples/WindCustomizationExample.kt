package com.maptiler.examples

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maptiler.maptilersdk.map.LngLat
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapView
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilerweather.MTRGBAColor
import com.maptiler.maptilerweather.MTWindLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Example demonstrating particle customization (density, speed, size, fade) for Wind.
 */
@Composable
fun WindCustomizationExample() {
    val context = LocalContext.current
    val controller = remember {
        MTMapViewController(context).apply {
            registerModule(MapTilerWeatherModule(context))
        }
    }

    var density by remember { mutableStateOf(2.0) }
    var particleSize by remember { mutableStateOf(1.5) }
    var speed by remember { mutableStateOf(0.001) }

    val windLayer = remember {
        MTWindLayer()
            .opacity(0.85)
            .color(MTRGBAColor(255, 255, 255, 200))
            .fastColor(MTRGBAColor(255, 80, 80, 255))
    }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 3.0
        )
    }

    // Debounced live update to layer options to prevent UI thread / JS bridge overload on fast slider drags
    LaunchedEffect(density, particleSize, speed) {
        delay(80)
        windLayer
            .density(density)
            .size(particleSize)
            .speed(speed)
            .updateParticleOptions()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MTMapView(
            modifier = Modifier.fillMaxSize(),
            referenceStyle = MTMapReferenceStyle.DATAVIZ,
            options = options,
            controller = controller,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Wind Particle Customization",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Text(
                    text = String.format(Locale.US, "Density: %.1f", density),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = density.toFloat(),
                    onValueChange = { density = it.toDouble() },
                    valueRange = 0.5f..5.0f
                )

                Text(
                    text = String.format(Locale.US, "Particle Size: %.1f px", particleSize),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = particleSize.toFloat(),
                    onValueChange = { particleSize = it.toDouble() },
                    valueRange = 0.5f..4.0f
                )

                Text(
                    text = String.format(Locale.US, "Speed Factor: %.4f", speed),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = speed.toFloat(),
                    onValueChange = { speed = it.toDouble() },
                    valueRange = 0.0002f..0.003f
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        windLayer.addTo(controller)
    }
}
