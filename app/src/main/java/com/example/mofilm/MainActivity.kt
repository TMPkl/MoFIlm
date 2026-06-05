package com.example.mofilm

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mofilm.ui.screens.LibraryScreen
import com.example.mofilm.ui.screens.MainMenuScreen
import com.example.mofilm.ui.screens.ScannerScreen
import com.example.mofilm.ui.screens.SettingsScreen
import com.example.mofilm.ui.theme.MoFilmTheme
import com.example.mofilm.ui.viewmodels.FilmProcessViewModel
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isOpenCVLoaded = OpenCVLoader.initDebug()
        if (isOpenCVLoaded) {
            Log.d("OpenCV", "OpenCV loaded successfully. Version: ${Core.VERSION}")
        } else {
            Log.e("OpenCV", "OpenCV initialization failed")
        }

        enableEdgeToEdge()
        setContent {
            MoFilmTheme {
                MoFilmApp(isOpenCVLoaded)
            }
        }
    }
}

enum class AppScreen(val title: String, val icon: Int) {
    MAIN_MENU("MoFilm", R.drawable.ic_home),
    SCANNER("Film Scanner", R.drawable.ic_home),
    LIBRARY("Film Library", R.drawable.ic_favorite),
    SETTINGS("Settings", R.drawable.ic_account_box)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoFilmApp(isOpenCVLoaded: Boolean = false) {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.MAIN_MENU) }
    // Hoistujemy ViewModel, aby był współdzielony między zakładkami
    val filmViewModel: FilmProcessViewModel = viewModel()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(currentScreen.title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (currentScreen != AppScreen.MAIN_MENU) {
                        IconButton(onClick = { currentScreen = AppScreen.MAIN_MENU }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(AppScreen.SCANNER, AppScreen.MAIN_MENU, AppScreen.LIBRARY, AppScreen.SETTINGS)
                items.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        label = { Text(screen.title) },
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.icon),
                                contentDescription = screen.title
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                AppScreen.MAIN_MENU -> MainMenuScreen(
                    isOpenCVLoaded = isOpenCVLoaded,
                    onScannerClick = { currentScreen = AppScreen.SCANNER },
                    onLibraryClick = { currentScreen = AppScreen.LIBRARY },
                    onSettingsClick = { currentScreen = AppScreen.SETTINGS }
                )
                AppScreen.SCANNER -> ScannerScreen(viewModel = filmViewModel)
                AppScreen.LIBRARY -> LibraryScreen(viewModel = filmViewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = filmViewModel)
            }
        }
    }
}
