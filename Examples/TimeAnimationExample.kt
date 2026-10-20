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
import com.maptiler.maptilersdk.map.LngLat
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapView
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle
import com.maptiler.maptilerweather.MTTemperatureLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Example demonstrating weather time animation and speed adjustment.
 */
@Composable
fun TimeAnimationExample() {
    val context = LocalContext.current
    val controller = remember {
        MTMapViewController(context).apply {
            registerModule(MapTilerWeatherModule(context))
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var currentSpeedFactor by remember { mutableStateOf(1.0) }
    var currentDate by remember { mutableStateOf<Date?>(null) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    val options = remember {
        MTMapOptions(
            center = LngLat(9.0, 48.0),
            zoom = 3.0
        )
    }

    val layer = remember {
        MTTemperatureLayer()
            .opacity(0.85)
            .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_TURBO))
    }

    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault()) }

    // Periodically sync animation time state from controller
    LaunchedEffect(isPlaying) {
        while (true) {
            coroutineScope.launch {
                if (startDate == null) startDate = layer.getAnimationStartDate()
                if (endDate == null) endDate = layer.getAnimationEndDate()
                currentDate = layer.getAnimationTimeDate()
            }
            delay(250)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MTMapView(
            modifier = Modifier.fillMaxSize(),
            referenceStyle = MTMapReferenceStyle.DATAVIZ,
            options = options,
            controller = controller,
        )

        // Overlay with playback controls
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
                    text = "Forecast Time Animation",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Text(
                    text = currentDate?.let { dateFormatter.format(it) } ?: "Loading time data...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF0088FF)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            isPlaying = !isPlaying
                            layer.animateByFactor(if (isPlaying) 3600.0 * currentSpeedFactor else 0.0)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088FF))
                    ) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text("Speed:", color = Color.White)

                    listOf(1.0, 2.0, 5.0).forEach { factor ->
                        FilterChip(
                            selected = currentSpeedFactor == factor,
                            onClick = {
                                currentSpeedFactor = factor
                                if (isPlaying) {
                                    layer.animateByFactor(3600.0 * currentSpeedFactor)
                                }
                            },
                            label = { Text("${factor.toInt()}x") }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        layer.addTo(controller)
    }
}
