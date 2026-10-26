package com.maptiler.examples

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.maptiler.maptilerweather.MTTemperatureLayer
import com.maptiler.maptilerweather.MapTilerWeatherModule
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRamp
import com.maptiler.maptilerweather.colorramp.MTWeatherColorRampPreset
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Example demonstrating weather time animation and speed adjustment.
 */
@Composable
fun ForecastAnimationExample() {
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
            zoom = 3.0,
            eventLevel = MTEventLevel.ESSENTIAL,
        )
    }

    val layer = remember {
        MTTemperatureLayer()
            .opacity(0.85)
            .colorRamp(MTWeatherColorRamp(MTWeatherColorRampPreset.TEMPERATURE_TURBO))
            .timeInterpolation(true)
    }

    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault()) }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
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
        layer.onSourceReady = {
            coroutineScope.launch {
                startDate = layer.getAnimationStartDate()
                endDate = layer.getAnimationEndDate()
                val initialTime = layer.getAnimationTime()
                if (initialTime != null) {
                    currentDate = Date((initialTime * 1000).toLong())
                }
            }
        }
        layer.onTick = { timestampSec ->
            currentDate = Date((timestampSec * 1000).toLong())
        }
        layer.onAnimationTimeSet = { timestampSec ->
            currentDate = Date((timestampSec * 1000).toLong())
        }

        layer.addTo(controller)
    }
}
