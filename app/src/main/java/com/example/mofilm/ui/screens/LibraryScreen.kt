package com.example.mofilm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.mofilm.data.FilmProcess
import com.example.mofilm.data.FilmProcessWithScans
import com.example.mofilm.data.Scan
import com.example.mofilm.ui.viewmodels.FilmProcessViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LibraryScreen(viewModel: FilmProcessViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Biblioteka", "Wołanie filmu")
    var previewScan by remember { mutableStateOf<Scan?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LibraryTabContent(viewModel, onImageClick = { previewScan = it })
                1 -> FilmProcessTabContent(viewModel)
            }
        }

        // Full screen preview overlay
        if (previewScan != null) {
            FullScreenImagePreview(
                scan = previewScan!!,
                onDismiss = { previewScan = null },
                onDelete = {
                    viewModel.deleteScan(previewScan!!)
                    previewScan = null
                }
            )
        }
    }
}

@Composable
fun FullScreenImagePreview(scan: Scan, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = scan.uri,
                contentDescription = "Pełny podgląd",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale.coerceAtLeast(1f),
                        scaleY = scale.coerceAtLeast(1f),
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state)
                    .clickable { onDismiss() },
                contentScale = ContentScale.Fit
            )
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Zamknij", tint = Color.White)
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, "Usuń skan", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            if (scale > 1f) {
                Text(
                    text = "%.1fx".format(scale),
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun LibraryTabContent(viewModel: FilmProcessViewModel, onImageClick: (Scan) -> Unit) {
    val processesWithScans by viewModel.processesWithScans.collectAsStateWithLifecycle()

    if (processesWithScans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Biblioteka jest pusta", style = MaterialTheme.typography.titleMedium)
                Text(text = "Dodaj raport wołania i przypisz do niego skany.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            items(processesWithScans) { item ->
                ProcessGroupItem(item, onImageClick)
            }
        }
    }
}

@Composable
fun ProcessGroupItem(item: FilmProcessWithScans, onImageClick: (Scan) -> Unit) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.process.filmType,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.process.developer} • ${dateFormat.format(Date(item.process.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            SuggestionChip(
                onClick = { },
                label = { Text("${item.scans.size} zdjęć") },
                enabled = false
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (item.scans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Brak przypisanych skanów", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(item.scans) { scan ->
                    Card(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(scan) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = scan.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilmProcessTabContent(viewModel: FilmProcessViewModel) {
    val processes by viewModel.allProcesses.collectAsStateWithLifecycle()
    val filmTypes by viewModel.uniqueFilmTypes.collectAsStateWithLifecycle()
    val developers by viewModel.uniqueDevelopers.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProcess by remember { mutableStateOf<FilmProcess?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (processes.isEmpty()) {
            Text(
                text = "Brak raportów wołania.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(processes) { process ->
                    FilmProcessItem(
                        process = process,
                        onDelete = { viewModel.deleteProcess(process) },
                        onEdit = { editingProcess = process }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj raport")
        }

        if (showAddDialog) {
            AddFilmProcessDialog(
                filmTypes = filmTypes,
                developers = developers,
                onDismiss = { showAddDialog = false },
                onConfirm = { filmType, isPush, pushValue, dev, temp, time, dilution, desc, comm ->
                    viewModel.addProcess(filmType, isPush, pushValue, dev, temp, time, dilution, desc, comm)
                    showAddDialog = false
                }
            )
        }

        if (editingProcess != null) {
            AddFilmProcessDialog(
                filmTypes = filmTypes,
                developers = developers,
                initialProcess = editingProcess,
                onDismiss = { editingProcess = null },
                onConfirm = { filmType, isPush, pushValue, dev, temp, time, dilution, desc, comm ->
                    editingProcess?.let { oldProcess ->
                        viewModel.updateProcess(oldProcess.copy(
                            filmType = filmType,
                            isPushPull = isPush,
                            pushPullValue = pushValue,
                            developer = dev,
                            temperature = temp,
                            developingTime = time,
                            dilution = dilution,
                            processDescription = desc,
                            comments = comm
                        ))
                    }
                    editingProcess = null
                }
            )
        }
    }
}

@Composable
fun FilmProcessItem(process: FilmProcess, onDelete: () -> Unit, onEdit: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = process.filmType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edytuj", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(text = dateFormat.format(Date(process.date)), style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (process.isPushPull) {
                Text(text = "Push/Pull: ${process.pushPullValue ?: "Tak"}", color = MaterialTheme.colorScheme.primary)
            }
            
            Text(text = "Wywoływacz: ${process.developer} (${process.dilution})")
            Text(text = "Parametry: ${process.temperature}°C, ${process.developingTime}")
            
            if (process.processDescription.isNotEmpty()) {
                Text(text = "Opis: ${process.processDescription}", style = MaterialTheme.typography.bodyMedium)
            }
            
            if (process.comments.isNotEmpty()) {
                Text(
                    text = "Komentarz: ${process.comments}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFilmProcessDialog(
    filmTypes: List<String>,
    developers: List<String>,
    initialProcess: FilmProcess? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String?, String, Double, String, String, String, String) -> Unit
) {
    var filmType by remember(initialProcess) { mutableStateOf(initialProcess?.filmType ?: "") }
    var isPushPull by remember(initialProcess) { mutableStateOf(initialProcess?.isPushPull ?: false) }
    var pushPullValue by remember(initialProcess) { mutableStateOf(initialProcess?.pushPullValue ?: "") }
    var developer by remember(initialProcess) { mutableStateOf(initialProcess?.developer ?: "") }
    var temperature by remember(initialProcess) { mutableStateOf(initialProcess?.temperature?.toString() ?: "") }
    var developingTime by remember(initialProcess) { mutableStateOf(initialProcess?.developingTime ?: "") }
    var dilution by remember(initialProcess) { mutableStateOf(initialProcess?.dilution ?: "") }
    var processDescription by remember(initialProcess) { mutableStateOf(initialProcess?.processDescription ?: "") }
    var comments by remember(initialProcess) { mutableStateOf(initialProcess?.comments ?: "") }

    var filmExpanded by remember { mutableStateOf(false) }
    val filteredFilmTypes = filmTypes.filter { it.contains(filmType, ignoreCase = true) }

    var devExpanded by remember { mutableStateOf(false) }
    val filteredDevelopers = developers.filter { it.contains(developer, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProcess == null) "Nowy raport wołania" else "Edytuj raport") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = filmExpanded,
                        onExpandedChange = { filmExpanded = !filmExpanded }
                    ) {
                        OutlinedTextField(
                            value = filmType,
                            onValueChange = { 
                                filmType = it
                                filmExpanded = true
                            },
                            label = { Text("Rodzaj filmu") },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filmExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (filteredFilmTypes.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = filmExpanded,
                                onDismissRequest = { filmExpanded = false }
                            ) {
                                filteredFilmTypes.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            filmType = selectionOption
                                            filmExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPushPull, onCheckedChange = { isPushPull = it })
                        Text("Push/Pull")
                        if (isPushPull) {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = pushPullValue,
                                onValueChange = { pushPullValue = it },
                                label = { Text("Wartość") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = devExpanded,
                        onExpandedChange = { devExpanded = !devExpanded }
                    ) {
                        OutlinedTextField(
                            value = developer,
                            onValueChange = { 
                                developer = it
                                devExpanded = true
                            },
                            label = { Text("Wywoływacz") },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = devExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (filteredDevelopers.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = devExpanded,
                                onDismissRequest = { devExpanded = false }
                            ) {
                                filteredDevelopers.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            developer = selectionOption
                                            devExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = dilution, onValueChange = { dilution = it }, label = { Text("Rozcieńczenie") }) }
                item { OutlinedTextField(value = temperature, onValueChange = { temperature = it }, label = { Text("Temperatura (°C)") }) }
                item { OutlinedTextField(value = developingTime, onValueChange = { developingTime = it }, label = { Text("Czas wywoływania") }) }
                item { OutlinedTextField(value = processDescription, onValueChange = { processDescription = it }, label = { Text("Opis procesu") }, minLines = 2) }
                item { OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("Komentarz") }, minLines = 2) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        filmType,
                        isPushPull,
                        if (isPushPull) pushPullValue else null,
                        developer,
                        temperature.toDoubleOrNull() ?: 20.0,
                        developingTime,
                        dilution,
                        processDescription,
                        comments
                    )
                }
            ) {
                Text(if (initialProcess == null) "Dodaj" else "Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
