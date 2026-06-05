package com.example.mofilm.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mofilm.ui.viewmodels.FilmProcessViewModel

enum class SettingsView {
    MAIN, FILMS, DEVELOPERS
}

@Composable
fun SettingsScreen(viewModel: FilmProcessViewModel = viewModel()) {
    var currentView by remember { mutableStateOf(SettingsView.MAIN) }

    BackHandler(enabled = currentView != SettingsView.MAIN) {
        currentView = SettingsView.MAIN
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (currentView) {
            SettingsView.MAIN -> MainSettingsList(onNavigate = { currentView = it })
            SettingsView.FILMS -> DictionaryManagementView(
                title = "Filmy",
                items = viewModel.uniqueFilmTypes.collectAsStateWithLifecycle().value,
                onUpdate = { old, new -> viewModel.updateFilmType(old, new) },
                onBack = { currentView = SettingsView.MAIN }
            )
            SettingsView.DEVELOPERS -> DictionaryManagementView(
                title = "Wywoływacze",
                items = viewModel.uniqueDevelopers.collectAsStateWithLifecycle().value,
                onUpdate = { old, new -> viewModel.updateDeveloper(old, new) },
                onBack = { currentView = SettingsView.MAIN }
            )
        }
    }
}

@Composable
fun MainSettingsList(onNavigate: (SettingsView) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ustawienia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Zmień nazwę",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(SettingsView.FILMS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Film")
                        }
                        Button(
                            onClick = { onNavigate(SettingsView.DEVELOPERS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Wywoływacz")
                        }
                    }
                }
            }
        }

        item {
            ListItem(
                headlineContent = { Text("O aplikacji") },
                supportingContent = { Text("MoFilm v1.0") },
                modifier = Modifier.clickable { }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryManagementView(
    title: String,
    items: List<String>,
    onUpdate: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var editingItem by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                }
            }
        )

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Brak danych", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingItem = item
                                newName = item
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Edit, contentDescription = "Edytuj", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edytuj nazwę") },
            text = {
                Column {
                    Text("Zmieniając tę nazwę, zaktualizujesz wszystkie wpisy w bibliotece.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nowa nazwa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val old = editingItem
                        if (old != null && newName.isNotBlank() && old != newName) {
                            onUpdate(old, newName)
                        }
                        editingItem = null
                    }
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
