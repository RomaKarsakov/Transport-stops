package com.example.project

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.project.ui.theme.ProjectTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

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

data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

data class ApiResponse(
    val coordinates: List<Coordinate>
)

class TransportRepository {
    private val client = OkHttpClient()
    private val baseUrl = "http://ykengo.ru:6969/stops"
    private val gson = Gson()
    suspend fun getCoordinates(type: String, routeNumber: String): List<Pair<Double, Double>> {
        val request = Request.Builder()
            .url("$baseUrl/$routeNumber")
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Ошибка: ${response.code}")

            val json = response.body?.string() ?: throw IOException("Пустой ответ")
            parseCoordinates(json)
        }
    }

    private fun parseCoordinates(json: String): List<Pair<Double, Double>> {
        return try {
            val response = gson.fromJson(json, ApiResponse::class.java)

            response.coordinates.map { coord ->
                coord.latitude to coord.longitude
            }
        } catch (e: JsonSyntaxException) {
            Log.e("JSON_PARSE", "Invalid JSON format", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("PARSE", "Unexpected parsing error", e)
            emptyList()
        }
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { themeManager.toggleTheme() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Toggle theme"
                )
            }
        }
        
        Text(
            text = "Выберите транспорт",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        val transportTypes = listOf(
            "автобус" to "Автобусы",
            "троллейбус" to "Троллейбусы",
            "трамвай" to "Трамваи"
        )

        transportTypes.forEach { (route, label) ->
            ElevatedButton(
                onClick = { navController.navigate("screen2/$route") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen2(navController: NavController, title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CenterAlignedTopAppBar(
            title = { 
                Text(
                    "Выберите $title",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        var numbers = when (title) {
            "автобус" -> arrayOf("40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63","64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83","84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99")
            "троллейбус" -> arrayOf("25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39")
            "трамвай" -> arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "23", "24")
            else -> arrayOf("")
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(numbers) { number ->
                ElevatedButton(
                    onClick = { navController.navigate("screen3/$title/$number") },
                    modifier = Modifier.aspectRatio(1f)
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen3(navController: NavController, type: String, title: String) {
    val viewModel: TransportViewModel = viewModel()
    val coordinates by viewModel.coordinates.collectAsState()

    LaunchedEffect(type, title) {
        viewModel.startUpdates(type, title)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CenterAlignedTopAppBar(
            title = { 
                Text(
                    text = "Маршрут №$title",
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            OsmMap(
                coordinates = coordinates,
                routeNumber = title
            )
        }
    }
}