package com.maptiler.weatherdemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maptiler.maptilersdk.MTConfig
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
import com.maptiler.maptilerweather.MTPrecipitationLayer
import com.maptiler.maptilerweather.MTPressureLayer
import com.maptiler.maptilerweather.MTRadarLayer
import com.maptiler.maptilerweather.MTTemperatureLayer
import com.maptiler.maptilerweather.MTWeatherLayer
import com.maptiler.maptilerweather.MTWindLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MTConfig.apiKey = BuildConfig.MAPTILER_API_KEY

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherDemoApp()
                }
            }
        }
    }
}

enum class ExampleType(val title: String) {
    PRECIPITATION("Precipitation"),
    PRESSURE("Pressure"),
    RADAR("Radar"),
    TEMPERATURE("Temperature"),
    WIND("Wind")
}

@Composable
fun WeatherDemoApp() {
    var selectedExample by remember { mutableStateOf(ExampleType.PRECIPITATION) }
    var expanded by remember { mutableStateOf(false) }
    
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
            eventLevel = MTEventLevel.ALL,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MTMapView(
            modifier = Modifier.fillMaxSize(),
            referenceStyle = MTMapReferenceStyle.DATAVIZ,
            options = options,
            controller = controller,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Button(onClick = { expanded = true }) {
                Text(selectedExample.title)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ExampleType.values().forEach { example ->
                    DropdownMenuItem(
                        text = { Text(example.title) },
                        onClick = {
                            selectedExample = example
                            expanded = false
                        }
                    )
                }
            }
        }

        var isMapReady by remember { mutableStateOf(false) }
        var currentActiveLayer by remember { mutableStateOf<MTWeatherLayer?>(null) }

        var pickedValue by remember { mutableStateOf<String?>(null) }
        var pickedLocation by remember { mutableStateOf<LngLat?>(null) }
        var currentMarker by remember { mutableStateOf<MTMarker?>(null) }

        LaunchedEffect(controller) {
            controller.delegate = object : MTMapViewDelegate {
                override fun onMapViewInitialized() {
                    isMapReady = true
                }

                override fun onEventTriggered(event: MTEvent, data: MTData?) {
                    if (event == MTEvent.ON_READY) {
                        isMapReady = true
                    }

                    if (event == MTEvent.ON_TAP) {
                        val coordinate = data?.coordinate
                        val activeLayer = currentActiveLayer
                        
                        if (coordinate == null) {
                            return
                        }
                        
                        if (activeLayer == null) {
                            return
                        }

                        controller.scope?.launch {
                            try {
                                val valueStr = when (activeLayer) {
                                    is MTPrecipitationLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(java.util.Locale.US, "%.1f mm/h", it.value) }
                                    }
                                    is MTPressureLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(java.util.Locale.US, "%.0f hPa", it.value) }
                                    }
                                    is MTRadarLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(java.util.Locale.US, "%.1f dBZ", it.value) }
                                    }
                                    is MTTemperatureLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(java.util.Locale.US, "%.1f °C (%.1f °F)", it.value, it.valueImperial) }
                                    }
                                    is MTWindLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(java.util.Locale.US, "%.1f km/h (%s)", it.speedKilometersPerHour, it.compassDirection) }
                                    }
                                    else -> {
                                        null
                                    }
                                } ?: "No data"

                                pickedValue = valueStr
                                pickedLocation = coordinate

                                // Remove old marker
                                currentMarker?.let {
                                    controller.style?.removeMarker(it)
                                }

                                // Place new marker
                                val marker = MTMarker(coordinate)
                                controller.style?.addMarker(marker)
                                currentMarker = marker
                            } catch (e: Exception) {
                                pickedValue = "Error: ${e.localizedMessage}"
                                pickedLocation = coordinate
                            }
                        }
                    }
                }
            }
        }

        // Handle layer loading
        LaunchedEffect(selectedExample, isMapReady) {
            if (!isMapReady) return@LaunchedEffect

            // Remove the previously active layer if there is one
            currentActiveLayer?.let {
                it.removeFrom(controller)
            }

            // Clear picked data when changing examples
            pickedValue = null
            pickedLocation = null
            currentMarker?.let {
                controller.style?.removeMarker(it)
                currentMarker = null
            }

            // Instantiate and add the new layer
            val newLayer = when (selectedExample) {
                ExampleType.PRECIPITATION -> {
                    MTPrecipitationLayer()
                        .opacity(0.8)
                        .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.PRECIPITATION))
                }
                ExampleType.PRESSURE -> {
                    MTPressureLayer()
                        .opacity(0.8)
                        .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.PRESSURE))
                }
                ExampleType.RADAR -> {
                    MTRadarLayer()
                        .opacity(0.8)
                        .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.RADAR))
                }
                ExampleType.TEMPERATURE -> {
                    MTTemperatureLayer()
                        .opacity(0.8)
                        .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_TURBO))
                }
                ExampleType.WIND -> {
                    MTWindLayer()
                        .opacity(0.8)
                        .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.WIND_VIRIDIS))
                }
            }

            newLayer.addTo(controller)
            currentActiveLayer = newLayer
        }

        DisposableEffect(controller) {
            onDispose {
                controller.delegate = null
            }
        }

        // Floating Card to display picked weather details
        if (pickedValue != null && pickedLocation != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = selectedExample.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pickedValue!!,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(
                                    java.util.Locale.US,
                                    "Lat: %.4f, Lng: %.4f",
                                    pickedLocation!!.lat,
                                    pickedLocation!!.lng
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = {
                                pickedValue = null
                                pickedLocation = null
                                currentMarker?.let {
                                    controller.style?.removeMarker(it)
                                    currentMarker = null
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
            }
        }
    }
}
