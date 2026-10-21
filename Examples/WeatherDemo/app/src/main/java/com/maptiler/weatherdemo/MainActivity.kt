package com.maptiler.weatherdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maptiler.examples.CustomColorRampExample
import com.maptiler.examples.LayerOrderingExample
import com.maptiler.examples.LayerSwitchingExample
import com.maptiler.examples.PickAtExample
import com.maptiler.examples.PrecipitationExample
import com.maptiler.examples.PressureExample
import com.maptiler.examples.RadarExample
import com.maptiler.examples.TemperatureExample
import com.maptiler.examples.TimeAnimationExample
import com.maptiler.examples.WindCustomizationExample
import com.maptiler.examples.WindExample
import com.maptiler.maptilersdk.MTConfig

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

enum class ExamplePage(val title: String) {
    CUSTOM_COLOR_RAMP("Custom Color Ramp"),
    LAYER_ORDERING("Layer Ordering"),
    LAYER_SWITCHING("Layer Switching"),
    PICK_AT("Pick At"),
    PRECIPITATION("Precipitation"),
    PRESSURE("Pressure"),
    RADAR("Radar"),
    TEMPERATURE("Temperature"),
    TIME_ANIMATION("Time Animation"),
    WIND_CUSTOMIZATION("Wind Customization"),
    WIND("Wind")
}

@Composable
fun WeatherDemoApp() {
    var selectedPage by remember { mutableStateOf(ExamplePage.PRECIPITATION) }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedPage) {
            ExamplePage.CUSTOM_COLOR_RAMP -> CustomColorRampExample()
            ExamplePage.LAYER_ORDERING -> LayerOrderingExample()
            ExamplePage.LAYER_SWITCHING -> LayerSwitchingExample()
            ExamplePage.PICK_AT -> PickAtExample()
            ExamplePage.PRECIPITATION -> PrecipitationExample()
            ExamplePage.PRESSURE -> PressureExample()
            ExamplePage.RADAR -> RadarExample()
            ExamplePage.TEMPERATURE -> TemperatureExample()
            ExamplePage.TIME_ANIMATION -> TimeAnimationExample()
            ExamplePage.WIND_CUSTOMIZATION -> WindCustomizationExample()
            ExamplePage.WIND -> WindExample()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = MapTilerBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(selectedPage.title, fontWeight = FontWeight.Bold)
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
                ExamplePage.values().forEach { page ->
                    DropdownMenuItem(
                        text = { Text(page.title) },
                        onClick = {
                            selectedPage = page
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
