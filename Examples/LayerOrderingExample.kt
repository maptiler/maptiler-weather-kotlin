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
import com.maptiler.maptilerweather.MTRadarLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule

enum class BaseStyleOption(val title: String, val referenceStyle: MTMapReferenceStyle) {
    DATAVIZ("Dataviz", MTMapReferenceStyle.DATAVIZ),
    BACKDROP("Backdrop", MTMapReferenceStyle.BACKDROP),
    SATELLITE("Satellite", MTMapReferenceStyle.SATELLITE)
}

/**
 * Example demonstrating weather layer placement (`beforeId`) relative to map style layers.
 */
@Composable
fun LayerOrderingExample() {
    val context = LocalContext.current
    val controller = remember {
        MTMapViewController(context).apply {
            registerModule(MapTilerWeatherModule(context))
        }
    }

    var selectedStyle by remember { mutableStateOf(BaseStyleOption.DATAVIZ) }
    var renderBehindLabels by remember { mutableStateOf(true) }

    val radarLayer = remember {
        MTRadarLayer().opacity(0.85)
    }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 4.0
        )
    }

    fun updateLayerOrdering() {
        radarLayer.beforeId(if (renderBehindLabels) "waterway-label" else null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MTMapView(
            modifier = Modifier.fillMaxSize(),
            referenceStyle = selectedStyle.referenceStyle,
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
                    text = "Layer Ordering & Base Style",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BaseStyleOption.values().forEach { option ->
                        FilterChip(
                            selected = selectedStyle == option,
                            onClick = { selectedStyle = option },
                            label = { Text(option.title) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Render Layer Beneath Labels", color = Color.White)
                    Switch(
                        checked = renderBehindLabels,
                        onCheckedChange = { checked ->
                            renderBehindLabels = checked
                            updateLayerOrdering()
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        updateLayerOrdering()
        radarLayer.addTo(controller)
    }
}
