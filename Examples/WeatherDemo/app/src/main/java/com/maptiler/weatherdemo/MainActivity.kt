package com.maptiler.weatherdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maptiler.maptilersdk.MTConfig
import com.maptiler.maptilersdk.map.MTMapOptions
import com.maptiler.maptilersdk.map.MTMapView
import com.maptiler.maptilersdk.map.MTMapViewController
import com.maptiler.maptilersdk.map.style.MTMapReferenceStyle

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
                    val context = LocalContext.current
                    val controller = remember { MTMapViewController(context) }

                    MTMapView(
                        referenceStyle = MTMapReferenceStyle.STREETS,
                        options = MTMapOptions(),
                        controller = controller,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Once the weather library is implemented, we will register it something like this:
                    // controller.registerModule(MapTilerWeatherModule())
                    // val windLayer = WindLayer()
                    // windLayer.addTo(controller)
                }
            }
        }
    }
}
