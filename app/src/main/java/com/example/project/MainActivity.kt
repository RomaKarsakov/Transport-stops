package com.example.project

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.ArraySet
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project.ui.theme.ProjectTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import java.util.ArrayList
import android.webkit.WebSettings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import com.example.project.ThemeManager

class MainActivity : ComponentActivity() {
    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeManager = ThemeManager(this)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        
        setContent {
            ProjectTheme(darkTheme = themeManager.isDarkTheme) {
                App(themeManager)
            }
        }
    }
}

class TransportRepository {
    private val client = OkHttpClient()
    private val baseUrl = "http://192.168.0.31:3000"

    suspend fun getCoordinates(type: String, routeNumber: String): List<Pair<Double, Double>> {
        val request = Request.Builder()
            .url("$baseUrl/$type/$routeNumber")
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Ошибка: ${response.code}")

            val json = response.body?.string() ?: throw IOException("Пустой ответ")
            parseCoordinates(json)
        }
    }

    private fun parseCoordinates(json: String): List<Pair<Double, Double>> {
        val type = object : TypeToken<List<List<Double>>>() {}.type
        return Gson().fromJson<List<List<Double>>>(json, type)
            .map { it[0] to it[1] }
    }
}


class TransportViewModel : ViewModel() {
    private val repository = TransportRepository()

    private val _coordinates = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())

    val coordinates: StateFlow<List<Pair<Double, Double>>> = _coordinates

    private var updateJob: Job? = null

    fun startUpdates(type: String, routeNumber: String) {
        updateJob?.cancel()

        updateJob = viewModelScope.launch {
            while (true) {
                try {
                    _coordinates.value = repository.getCoordinates(type, routeNumber)
                } catch (e: Exception) {
                    Log.e("Transport", "Ошибка обновления", e)
                }
                delay(10000)
            }
        }
    }
}


@Composable
fun OsmMap(coordinates: List<Pair<Double, Double>>, routeNumber: String) {
    val mapView = rememberMapView()

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        with(view) {

            view.overlays.clear()
            coordinates.forEach { (lat, lon) ->
                Marker(this).apply {
                    position = GeoPoint(lat, lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    overlays.add(this)
                }
            }
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            invalidate()
        }
    }
    LaunchedEffect(Unit) {
        mapView.apply {
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(56.8519, 60.6122))
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
}

@Composable
fun rememberMapView(): MapView {
    val context = LocalContext.current
    Configuration.getInstance().apply {
        userAgentValue = context.packageName

    }
    return remember {
        MapView(context).apply {
            Configuration.getInstance().userAgentValue = context.packageName
        }
    }

}

@Composable
fun App(themeManager: ThemeManager) {
    val navController = rememberNavController()
    Scaffold { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "screen1",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("screen1") {
                Screen1(navController, themeManager)
            }
            composable("screen2/{title}") { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: ""
                Screen2(
                    title = title,
                    navController = navController
                )
            }
            composable("screen3/{type}/{title}") { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: ""
                Screen3(
                    type = type,
                    title = title,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun Screen1(navController: NavController, themeManager: ThemeManager) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Button(
            onClick = { themeManager.toggleTheme() }
        ) {
            Text(if (themeManager.isDarkTheme) "Светлая" else "Темная")
        }
        Text("Выберите транспорт:")
        Button(
            onClick = { navController.navigate("screen2/автобус") },
        ) {
            Text("автобус")
        }
        Button(
            onClick = { navController.navigate("screen2/троллейбус") },
        ) {
            Text("троллейбус")
        }
        Button(
            onClick = { navController.navigate("screen2/трамвай") },
        ) {
            Text("трамвай")
        }

    }
}

@Composable
fun Screen2(navController: NavController, title: String) {
    Column (
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text("Выберите $title")
        Button(
            onClick = { navController.popBackStack() },
        ) {
            Text("Назад")
        }
        var numbers = arrayOf("")
        when (title){
            "автобус" -> {
                numbers = arrayOf("06", "24", "24с", "25", "28", "40", "41", "42", "43", "043", "44", "45", "46", "47", "48", "49", "50", "51", "52", "053", "53", "054", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "85", "86", "87", "88", "89", "90", "91", "91м", "92", "93", "94", "95", "96", "96б", "97", "98м", "98", "99")
            }
            "троллейбус" -> {
                numbers = arrayOf("25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39")
            }
            "трамвай" -> {
                numbers = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "23", "24")
            }
        }
        Column (
            modifier = Modifier.verticalScroll(rememberScrollState())
        ){
            for(el in numbers){
                Button(
                    onClick = { navController.navigate("screen3/$title/$el") },
                ) {
                    Text(el)
                }
            }
        }



    }
}


@Composable
fun Screen3(navController: NavController,type: String, title: String){
    val viewModel: TransportViewModel = viewModel()

    val coordinates by viewModel.coordinates.collectAsState()

    LaunchedEffect(type, title) {
        viewModel.startUpdates(type, title)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(title)
        Button(
            onClick = { navController.popBackStack() },
        ) {
            Text("Назад")
        }
        Box(modifier = Modifier) {
            OsmMap(coordinates = coordinates,
                routeNumber = title
            )
        }
    }
}