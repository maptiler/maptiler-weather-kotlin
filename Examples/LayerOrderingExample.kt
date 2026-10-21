package com.maptiler.examples

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.maptiler.maptilersdk.map.options.MTEventLevel
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilerweather.MTRadarLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule

/**
 * Base styles with the ID of their lowest label layer. If the ID is not found in the loaded style,
 * the SDK falls back to the first symbol (label) layer.
 */
enum class BaseStyleOption(
    val title: String,
    val referenceStyle: MTMapReferenceStyle,
    val labelLayerId: String,
) {
    DATAVIZ("Dataviz", MTMapReferenceStyle.DATAVIZ, "Ocean labels"),
    BACKDROP("Backdrop", MTMapReferenceStyle.BACKDROP, "Ocean labels"),
    SATELLITE("Satellite", MTMapReferenceStyle.SATELLITE, "Pathway labels")
}

/**
 * Example demonstrating weather layer placement (`beforeId`) relative to map style layers.
 */
@Composable
fun LayerOrderingExample() {
    val context = LocalContext.current
    var selectedStyle by remember { mutableStateOf(BaseStyleOption.DATAVIZ) }
    var renderBehindLabels by remember { mutableStateOf(true) }

    val controller = remember(selectedStyle) {
        MTMapViewController(context).apply {
            registerModule(MapTilerWeatherModule(context))
        }
    }

    val radarLayer = remember(selectedStyle) {
        MTRadarLayer().opacity(0.85)
    }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 4.0,
            eventLevel = MTEventLevel.ESSENTIAL,
        )
    }

    // Beneath labels: insert before the lowest label layer. Above labels: null moves the layer to the top.
    fun updateLayerOrdering() {
        radarLayer.beforeId(if (renderBehindLabels) selectedStyle.labelLayerId else null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key(selectedStyle) {
            MTMapView(
                modifier = Modifier.fillMaxSize(),
                referenceStyle = selectedStyle.referenceStyle,
                options = options,
                controller = controller,
            )
        }

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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
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
                        onCheckedChange = { renderBehindLabels = it }
                    )
                }
            }
        }
    }

    LaunchedEffect(selectedStyle) {
        // The layer is added asynchronously once the weather module is loaded, so apply
        // the current ordering when it is ready. A null beforeId on add means default placement, not top.
        radarLayer.onSourceReady = { updateLayerOrdering() }
        radarLayer.addTo(controller)
    }

    // Keyed on the layer too, so the switch always acts on the layer of the current base style.
    LaunchedEffect(radarLayer, renderBehindLabels) {
        updateLayerOrdering()
    }
}
