package com.maptiler.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maptiler.maptilersdk.annotations.MTMarker
import com.maptiler.maptilersdk.events.MTEvent
import com.maptiler.maptilersdk.map.LngLat
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapView
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.MTMapViewDelegate
import com.maptiler.maptilersdk.map.options.MTEventLevel
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilersdk.map.types.MTData
import com.maptiler.maptilerweather.MTTemperatureLayer
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.coroutines.launch

/**
 * Example demonstrating how to use the `pickAt` feature in the MapTiler Weather SDK.
 * This displays a temperature layer, listens to map tap events, queries the weather data
 * at the tapped location, and displays a marker and popup card with the temperature values.
 */
@Composable
fun PickAtExample() {
    val context = LocalContext.current
    val controller = remember { MTMapViewController(context) }
    val coroutineScope = rememberCoroutineScope()

    // State to hold picked temperature information
    var pickedTemp by remember { mutableStateOf<String?>(null) }
    var pickedCoordinates by remember { mutableStateOf<LngLat?>(null) }
    var isQuerying by remember { mutableStateOf(false) }
    var currentMarker by remember { mutableStateOf<MTMarker?>(null) }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 3.0,
            // We must set MTEventLevel.ALL to receive ON_TAP gesture events from the map
            eventLevel = MTEventLevel.ALL,
        )
    }

    val tempLayer = remember {
        MTTemperatureLayer()
            .opacity(0.8)
            .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_TURBO))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MTMapView(
            modifier = Modifier.fillMaxSize(),
            referenceStyle = MTMapReferenceStyle.DATAVIZ,
            options = options,
            controller = controller,
        )

        // Overlay with Picked Temperature details
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Weather Data Picker (pickAt)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    if (isQuerying) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Querying weather data...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    } else if (pickedTemp != null && pickedCoordinates != null) {
                        Text(
                            text = "Location: Lat ${String.format("%.4f", pickedCoordinates!!.lat)}, Lng ${String.format("%.4f", pickedCoordinates!!.lng)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Temperature: $pickedTemp",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Green
                        )
                    } else {
                        Text(
                            text = "Tap anywhere on the map to query the temperature.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(controller) {
        // Add the temperature layer to the map
        tempLayer.addTo(controller)

        // Register the map delegate to listen to gesture events
        controller.delegate = object : MTMapViewDelegate {
            override fun onMapViewInitialized() {
                // Map is ready
            }

            override fun onEventTriggered(event: MTEvent, data: MTData?) {
                if (event == MTEvent.ON_TAP) {
                    val coordinate = data?.coordinate ?: return

                    coroutineScope.launch {
                        isQuerying = true
                        pickedCoordinates = coordinate

                        try {
                            // Call pickAt on the active weather layer
                            val res = tempLayer.pickAt(coordinate.lng, coordinate.lat)
                            if (res != null) {
                                pickedTemp = String.format(
                                    java.util.Locale.US,
                                    "%.1f °C (%.1f °F)",
                                    res.value,
                                    res.valueImperial
                                )
                            } else {
                                pickedTemp = "No data available at this location"
                            }

                            // Manage markers on the map style
                            currentMarker?.let {
                                controller.style?.removeMarker(it)
                            }
                            val marker = MTMarker(coordinate)
                            controller.style?.addMarker(marker)
                            currentMarker = marker

                        } catch (e: Exception) {
                            pickedTemp = "Error: ${e.localizedMessage}"
                        } finally {
                            isQuerying = false
                        }
                    }
                }
            }
        }
    }
}
