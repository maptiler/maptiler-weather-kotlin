package com.maptiler.weatherdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val MapTilerBlue = Color(0xFF0088FF)
val MapTilerBlueDark = Color(0xFF0066CC)
val MapTilerBlueContainer = Color(0xFFE5F1FF)

private val MapTilerColorScheme = lightColorScheme(
    primary = MapTilerBlue,
    onPrimary = Color.White,
    primaryContainer = MapTilerBlueContainer,
    onPrimaryContainer = MapTilerBlueDark,
    secondary = MapTilerBlue,
    onSecondary = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MTConfig.apiKey = BuildConfig.MAPTILER_API_KEY

        setContent {
            MaterialTheme(colorScheme = MapTilerColorScheme) {
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

    var isMapReady by remember { mutableStateOf(false) }
    var currentActiveLayer by remember { mutableStateOf<MTWeatherLayer?>(null) }

    var pickedValue by remember { mutableStateOf<String?>(null) }
    var pickedLocation by remember { mutableStateOf<LngLat?>(null) }
    var currentMarker by remember { mutableStateOf<MTMarker?>(null) }

    // Animation state
    var isPlaying by remember { mutableStateOf(false) }
    var currentAnimTimeSec by remember { mutableStateOf<Double?>(null) }
    var animStartSec by remember { mutableStateOf<Double?>(null) }
    var animEndSec by remember { mutableStateOf<Double?>(null) }
    var selectedSpeedFactor by remember { mutableStateOf(1.0) }

    Box(modifier = Modifier.fillMaxSize()) {
        MTMapView(
            modifier = Modifier.fillMaxSize(),
            referenceStyle = MTMapReferenceStyle.DATAVIZ,
            options = options,
            controller = controller,
        )

        // Layer Selector Dropdown at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = MapTilerBlue)
            ) {
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

                        if (coordinate == null || activeLayer == null) {
                            return
                        }

                        controller.scope?.launch {
                            try {
                                val valueStr = when (activeLayer) {
                                    is MTPrecipitationLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(Locale.US, "%.1f mm/h", it.value) }
                                    }
                                    is MTPressureLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(Locale.US, "%.0f hPa", it.value) }
                                    }
                                    is MTRadarLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(Locale.US, "%.1f dBZ", it.value) }
                                    }
                                    is MTTemperatureLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(Locale.US, "%.1f °C (%.1f °F)", it.value, it.valueImperial) }
                                    }
                                    is MTWindLayer -> {
                                        val res = activeLayer.pickAt(coordinate.lng, coordinate.lat)
                                        res?.let { String.format(Locale.US, "%.1f km/h (%s)", it.speedKilometersPerHour, it.compassDirection) }
                                    }
                                    else -> null
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

        // Handle layer loading & animation callbacks setup
        LaunchedEffect(selectedExample, isMapReady) {
            if (!isMapReady) return@LaunchedEffect

            // Remove previous active layer
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

            // Reset animation state variables
            animStartSec = null
            animEndSec = null
            currentAnimTimeSec = null
            isPlaying = false

            // Instantiate layer
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

            // Register animation callbacks
            newLayer.onPlayAnimation = { time ->
                isPlaying = true
                currentAnimTimeSec = time
            }
            newLayer.onPauseAnimation = { time ->
                isPlaying = false
                currentAnimTimeSec = time
            }
            newLayer.onTick = { time ->
                currentAnimTimeSec = time
            }
            newLayer.onAnimationTimeSet = { time ->
                currentAnimTimeSec = time
            }

            val syncAnimationMetaData: suspend () -> Unit = {
                val start = newLayer.getAnimationStart()
                val end = newLayer.getAnimationEnd()
                val current = newLayer.getAnimationTime()
                val playing = newLayer.isPlaying()

                if (start != null && end != null) {
                    animStartSec = start
                    animEndSec = end
                }
                if (current != null) {
                    currentAnimTimeSec = current
                }
                if (playing != null) {
                    isPlaying = playing
                }
            }

            newLayer.onSourceReady = {
                controller.scope?.launch {
                    syncAnimationMetaData()
                }
            }

            newLayer.addTo(controller)
            currentActiveLayer = newLayer

            // Poll for metadata in case source ready fired early
            controller.scope?.launch {
                repeat(5) {
                    if (animStartSec == null || animEndSec == null) {
                        syncAnimationMetaData()
                        delay(400)
                    }
                }
            }
        }

        DisposableEffect(controller) {
            onDispose {
                controller.delegate = null
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Floating Card to display picked weather details (if tapped on map)
            if (pickedValue != null && pickedLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedExample.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MapTilerBlue,
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
                                    Locale.US,
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

            // Animation Control Bar Card
            AnimationControlBar(
                isPlaying = isPlaying,
                onPlayPauseToggle = {
                    val layer = currentActiveLayer ?: return@AnimationControlBar
                    if (isPlaying) {
                        layer.animate(0.0)
                        isPlaying = false
                    } else {
                        layer.animate(3600.0 * selectedSpeedFactor)
                        isPlaying = true
                    }
                },
                currentAnimTimeProvider = { currentAnimTimeSec },
                animStartSec = animStartSec,
                animEndSec = animEndSec,
                selectedSpeedFactor = selectedSpeedFactor,
                onSpeedFactorSelected = { factor ->
                    selectedSpeedFactor = factor
                    if (isPlaying) {
                        currentActiveLayer?.animate(3600.0 * factor)
                    }
                }
            )
        }
    }
}

@Composable
fun AnimationControlBar(
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    currentAnimTimeProvider: () -> Double?,
    animStartSec: Double?,
    animEndSec: Double?,
    selectedSpeedFactor: Double,
    onSpeedFactorSelected: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Play/Pause, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play / Pause Button
                Button(
                    onClick = onPlayPauseToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = MapTilerBlue),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(
                        text = if (isPlaying) "⏸ Pause" else "▶ Play",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Formatted Date & Time Header
                val currentAnimTimeSec = currentAnimTimeProvider()
                Text(
                    text = formatTimestamp(currentAnimTimeSec),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speed Control Buttons (0.5x, 1x, 2x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Speed:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(0.5, 1.0, 2.0).forEach { factor ->
                    val label = if (factor < 1.0) "${factor}x" else "${factor.toInt()}x"
                    val isSelected = selectedSpeedFactor == factor
                    if (isSelected) {
                        Button(
                            onClick = { onSpeedFactorSelected(factor) },
                            colors = ButtonDefaults.buttonColors(containerColor = MapTilerBlue),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(label, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSpeedFactorSelected(factor) },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = MapTilerBlue)
                        }
                    }
                }
            }

            val start = animStartSec
            val end = animEndSec
            if (start != null && end != null && end > start) {
                Spacer(modifier = Modifier.height(12.dp))

                val currentAnimTimeSec = currentAnimTimeProvider()
                val current = currentAnimTimeSec ?: start
                val progressFloat = ((current - start) / (end - start)).toFloat().coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatShortTime(start),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { progressFloat },
                        color = MapTilerBlue,
                        trackColor = MapTilerBlueContainer,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatShortTime(end),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val timestampFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm 'UTC'", Locale.US)
    .withZone(ZoneId.of("UTC"))

private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    .withZone(ZoneId.of("UTC"))

private fun formatTimestamp(timestampSec: Double?): String {
    if (timestampSec == null || timestampSec.isNaN() || timestampSec <= 0) return "Loading..."
    return timestampFormatter.format(Instant.ofEpochSecond(timestampSec.toLong()))
}

private fun formatShortTime(timestampSec: Double?): String {
    if (timestampSec == null || timestampSec.isNaN() || timestampSec <= 0) return "--:--"
    return shortTimeFormatter.format(Instant.ofEpochSecond(timestampSec.toLong()))
}
