package com.maptiler.weatherdemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maptiler.maptilersdk.MTConfig
import com.maptiler.maptilersdk.bridge.MTBridge
import com.maptiler.maptilersdk.bridge.MTCommand
import com.maptiler.maptilersdk.map.LngLat
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapView
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilerweather.MTPrecipitationLayer
import com.maptiler.maptilerweather.MTPressureLayer
import com.maptiler.maptilerweather.MTRadarLayer
import com.maptiler.maptilerweather.MTTemperatureLayer
import com.maptiler.maptilerweather.MTWeatherLayer
import com.maptiler.maptilerweather.MTWindLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset

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

        var currentActiveLayer by remember { mutableStateOf<MTWeatherLayer?>(null) }
        var isFirstLoad by remember { mutableStateOf(true) }

        LaunchedEffect(selectedExample) {
            if (isFirstLoad) {
                // For demo purposes
                kotlinx.coroutines.delay(1000)
                isFirstLoad = false
            }

            // Remove the previously active layer if there is one
            currentActiveLayer?.let {
                it.removeFrom(controller)
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
    }
}
