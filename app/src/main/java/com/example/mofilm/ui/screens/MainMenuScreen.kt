package com.example.mofilm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mofilm.R
import com.example.mofilm.ui.components.NavigationCard
import com.example.mofilm.ui.components.OpenCVStatusBadge

@Composable
fun MainMenuScreen(
    isOpenCVLoaded: Boolean,
    onScannerClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Status OpenCV jako mały indykator na górze
        OpenCVStatusBadge(isOpenCVLoaded)

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Wybierz moduł",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp)
        )

        // Zakładka / Przycisk: Film Scanner
        NavigationCard(
            title = "Film Scanner",
            description = "Skanuj i przetwarzaj negatywy",
            iconRes = R.drawable.ic_home,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onScannerClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Zakładka / Przycisk: Film Library
        NavigationCard(
            title = "Film Library",
            description = "Przeglądaj swoją bibliotekę filmów",
            iconRes = R.drawable.ic_favorite,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onLibraryClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Zakładka / Przycisk: Settings
        NavigationCard(
            title = "Settings",
            description = "Konfiguracja aplikacji",
            iconRes = R.drawable.ic_account_box,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = onSettingsClick
        )
    }
}
