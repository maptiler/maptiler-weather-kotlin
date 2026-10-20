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
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilerweather.MTRGBAColor
import com.maptiler.maptilerweather.MTTemperatureLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampStop

enum class ColorRampOption(val label: String) {
    TURBO("Preset: Turbo"),
    SPECTRAL("Preset: Spectral"),
    CUSTOM_HEATMAP("Custom Heatmap")
}

/**
 * Example demonstrating how to apply predefined or custom color ramps.
 */
@Composable
fun CustomColorRampExample() {
    val context = LocalContext.current
    val controller = remember {
        MTMapViewController(context).apply {
            registerModule(MapTilerWeatherModule(context))
        }
    }

    var selectedOption by remember { mutableStateOf(ColorRampOption.TURBO) }
    var isReversed by remember { mutableStateOf(false) }

    val customHeatmapRamp = remember {
        MTWeatherColorRamp(
            stops = listOf(
                MTWeatherColorRampStop(-20.0, MTRGBAColor(15, 32, 67, 255)),   // Deep blue (Freezing)
                MTWeatherColorRampStop(0.0, MTRGBAColor(100, 200, 255, 255)),  // Cyan (Zero)
                MTWeatherColorRampStop(15.0, MTRGBAColor(255, 255, 100, 255)), // Mild Yellow
                MTWeatherColorRampStop(30.0, MTRGBAColor(255, 128, 0, 255)),   // Warm Orange
                MTWeatherColorRampStop(45.0, MTRGBAColor(200, 0, 50, 255))     // Extreme Red
            )
        )
    }

    val layer = remember {
        MTTemperatureLayer().opacity(0.8)
    }

    fun applyColorRamp() {
        val baseRamp = when (selectedOption) {
            ColorRampOption.TURBO -> MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_TURBO)
            ColorRampOption.SPECTRAL -> MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_SPECTRAL)
            ColorRampOption.CUSTOM_HEATMAP -> customHeatmapRamp.clone()
        }
        val targetRamp = if (isReversed) baseRamp.reverse(clone = true) else baseRamp
        layer.colorRamp(targetRamp)
    }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 3.0
        )
    }

    LaunchedEffect(selectedOption, isReversed) {
        applyColorRamp()
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
                    text = "Color Ramp Styling",
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
                    ColorRampOption.values().forEach { option ->
                        FilterChip(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reverse Ramp Direction", color = Color.White)
                    Switch(
                        checked = isReversed,
                        onCheckedChange = { checked -> isReversed = checked }
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        applyColorRamp()
        layer.addTo(controller)
    }
}
