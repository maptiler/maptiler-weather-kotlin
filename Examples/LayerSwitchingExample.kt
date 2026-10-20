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
import com.maptiler.maptilerweather.MTPrecipitationLayer
import com.maptiler.maptilerweather.MTRadarLayer
import com.maptiler.maptilerweather.MTTemperatureLayer
import com.maptiler.maptilerweather.MTWeatherLayer
import com.maptiler.maptilerweather.MTWindLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule
import java.util.Locale

enum class ActiveWeatherType(val title: String) {
    TEMPERATURE("Temperature"),
    RADAR("Radar"),
    PRECIPITATION("Precipitation"),
    WIND("Wind")
}

/**
 * Example demonstrating how to dynamically switch between weather dataset layers
 * and control layer opacity and visibility in MapTiler Weather SDK.
 */
@Composable
fun LayerSwitchingExample() {
    val context = LocalContext.current
    val controller = remember {
        MTMapViewController(context).apply {
            registerModule(MapTilerWeatherModule(context))
        }
    }

    var selectedType by remember { mutableStateOf(ActiveWeatherType.TEMPERATURE) }
    var layerOpacity by remember { mutableStateOf(0.8f) }
    var currentActiveLayer by remember { mutableStateOf<MTWeatherLayer?>(null) }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 3.0
        )
    }

    fun switchLayer(type: ActiveWeatherType) {
        currentActiveLayer?.removeFrom(controller)

        val newLayer = when (type) {
            ActiveWeatherType.TEMPERATURE -> MTTemperatureLayer()
            ActiveWeatherType.RADAR -> MTRadarLayer()
            ActiveWeatherType.PRECIPITATION -> MTPrecipitationLayer()
            ActiveWeatherType.WIND -> MTWindLayer()
        }
        newLayer.opacity(layerOpacity.toDouble())
        newLayer.addTo(controller)
        currentActiveLayer = newLayer
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Layer Switching & Opacity Control",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActiveWeatherType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                switchLayer(type)
                            },
                            label = { Text(type.title) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = String.format(Locale.US, "Opacity: %d%%", (layerOpacity * 100).toInt()),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = layerOpacity,
                        onValueChange = { opacity ->
                            layerOpacity = opacity
                            currentActiveLayer?.setOpacity(opacity.toDouble())
                        },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        switchLayer(selectedType)
    }
}
