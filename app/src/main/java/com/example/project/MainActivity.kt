package com.example.project

import android.annotation.SuppressLint
import android.os.Bundle
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


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjectTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "screen1"
    ) {
        composable("screen1") {
            Screen1(navController)
        }
        composable("screen2/{title}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            Screen2(
                title = title,
                navController = navController
            )
        }
        composable("screen3/{title}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            Screen3(
                title = title,
                navController = navController
            )
        }
    }
}

@Composable
fun Screen1(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                    onClick = { navController.navigate("screen3/$el") },
                ) {
                    Text(el)
                }
            }
        }



    }
}


@Composable
fun Screen3(navController: NavController, title: String){
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
    }
}