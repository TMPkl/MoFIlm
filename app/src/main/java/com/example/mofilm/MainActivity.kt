package com.example.mofilm

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class AppScreen(val title: String, val icon: ImageVector) {
    MAIN_MENU("MoFilm", Icons.Default.Home),
    SCANNER("Film Scanner", Icons.Default.PhotoCamera),
    LIBRARY("Film Library", Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoFilmApp(isOpenCVLoaded: Boolean = false) {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.MAIN_MENU) }
    val filmViewModel: FilmProcessViewModel = viewModel()

    // Powrót do menu głównego przyciskiem systemowym
    BackHandler(enabled = currentScreen != AppScreen.MAIN_MENU) {
        currentScreen = AppScreen.MAIN_MENU
    }

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
                val items = listOf(
                    AppScreen.MAIN_MENU,
                    AppScreen.SCANNER,
                    AppScreen.LIBRARY,
                    AppScreen.SETTINGS
                )
                items.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        label = { Text(screen.title) },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
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
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
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
}
