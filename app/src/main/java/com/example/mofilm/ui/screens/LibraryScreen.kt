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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: FilmProcessViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Biblioteka", "Wołanie filmu")
    var previewScan by remember { mutableStateOf<Scan?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
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
                    Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error)
                }
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
                Icon(
                    Icons.Default.PhotoLibrary, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(text = "Biblioteka jest pusta", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Dodaj raport wołania i przypisz skany.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
            Column {
                Text(
                    text = item.process.filmType,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${item.process.developer} • ${dateFormat.format(Date(item.process.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SuggestionChip(
                onClick = { },
                label = { Text("${item.scans.size} zdjęć") },
                enabled = false,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (item.scans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Brak przypisanych skanów", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(item.scans) { scan ->
                    Card(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(scan) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
        
        HorizontalDivider(
            modifier = Modifier.padding(top = 24.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Brak raportów wołania.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
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
                onConfirm = { filmType, isPushPull, pushPullValue, developer, temperature, time, dilution, desc, comm ->
                    viewModel.addProcess(filmType, isPushPull, pushPullValue, developer, temperature, time, dilution, desc, comm)
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
                onConfirm = { filmType, isPushPull, pushPullValue, developer, temperature, time, dilution, desc, comm ->
                    viewModel.updateProcess(editingProcess!!.copy(
                        filmType = filmType,
                        isPushPull = isPushPull,
                        pushPullValue = pushPullValue,
                        developer = developer,
                        temperature = temperature,
                        developingTime = time,
                        dilution = dilution,
                        processDescription = desc,
                        comments = comm
                    ))
                    editingProcess = null
                }
            )
        }
    }
}

@Composable
fun FilmProcessItem(process: FilmProcess, onDelete: () -> Unit, onEdit: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = process.filmType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(process.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edytuj", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Usuń", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Wywoływacz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(process.developer, style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Temp.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("${process.temperature}°C", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Czas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(process.developingTime, style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Rozc.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(process.dilution, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (process.isPushPull) {
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Push/Pull: ${process.pushPullValue ?: ""}") },
                    enabled = false
                )
            }

            Text(
                text = "ID: ${process.id}",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (process.processDescription.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = process.processDescription, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    var temperature by remember(initialProcess) { mutableStateOf(initialProcess?.temperature?.toString() ?: "20.0") }
    
    val initialDilPart = initialProcess?.dilution?.replace("1+", "")?.replace("1:", "")?.trim() ?: ""
    var dilutionPart by remember(initialProcess) { mutableStateOf(initialDilPart) }
    
    val initialMin = initialProcess?.developingTime?.substringBefore(":") ?: ""
    val initialSec = initialProcess?.developingTime?.substringAfter(":", "00") ?: ""
    var timeMin by remember(initialProcess) { mutableStateOf(initialMin) }
    var timeSec by remember(initialProcess) { mutableStateOf(initialSec) }

    var processDescription by remember(initialProcess) { mutableStateOf(initialProcess?.processDescription ?: "") }
    var comments by remember(initialProcess) { mutableStateOf(initialProcess?.comments ?: "") }

    val focusSec = remember { FocusRequester() }

    var filmExpanded by remember { mutableStateOf(false) }
    val filteredFilmTypes = filmTypes.filter { it.contains(filmType, ignoreCase = true) }

    var devExpanded by remember { mutableStateOf(false) }
    val filteredDevelopers = developers.filter { it.contains(developer, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (initialProcess == null) "Nowy raport wołania" else "Edytuj raport",
                color = MaterialTheme.colorScheme.primary
            ) 
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filmExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = devExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
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
                
                item { 
                    OutlinedTextField(
                        value = dilutionPart, 
                        onValueChange = { input ->
                            if (input.all { char -> char.isDigit() }) {
                                if (input.isEmpty() || (input.toIntOrNull() ?: 0) <= 200) {
                                    dilutionPart = input
                                }
                            }
                        }, 
                        label = { Text("Rozcieńczenie") },
                        prefix = { Text("1 : ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    ) 
                }

                item {
                    Column {
                        Text(
                            "Czas wywoływania", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = timeMin,
                                onValueChange = { input ->
                                    if (input.all { char -> char.isDigit() }) {
                                        timeMin = input
                                        if (input.length >= 2) focusSec.requestFocus()
                                    }
                                },
                                label = { Text("Min") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(textAlign = TextAlign.Center)
                            )
                            Text(" : ", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = timeSec,
                                onValueChange = { input ->
                                    if (input.all { char -> char.isDigit() } && (input.toIntOrNull() ?: 0) < 60) {
                                        timeSec = input
                                    }
                                },
                                label = { Text("Sek") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f).focusRequester(focusSec),
                                textStyle = TextStyle(textAlign = TextAlign.Center)
                            )
                        }
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {
                                    val currentTotal = (timeMin.toIntOrNull() ?: 0) * 60 + (timeSec.toIntOrNull() ?: 0)
                                    val newTotal = (currentTotal - 30).coerceAtLeast(0)
                                    timeMin = (newTotal / 60).toString()
                                    timeSec = (newTotal % 60).toString().padStart(2, '0')
                                },
                                label = { Text("-30s") }
                            )
                            AssistChip(
                                onClick = {
                                    val currentTotal = (timeMin.toIntOrNull() ?: 0) * 60 + (timeSec.toIntOrNull() ?: 0)
                                    val newTotal = currentTotal + 30
                                    timeMin = (newTotal / 60).toString()
                                    timeSec = (newTotal % 60).toString().padStart(2, '0')
                                },
                                label = { Text("+30s") }
                            )
                        }
                    }
                }

                item { 
                    OutlinedTextField(
                        value = temperature, 
                        onValueChange = { temperature = it.replace(",", ".") }, 
                        label = { Text("Temperatura (°C)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    ) 
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
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                        }
                    }
                }

                item { OutlinedTextField(value = processDescription, onValueChange = { processDescription = it }, label = { Text("Opis procesu") }, minLines = 2, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)) }
                item { OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("Komentarz") }, minLines = 2, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalTime = "${timeMin.ifEmpty { "0" }.padStart(2, '0')}:${timeSec.ifEmpty { "0" }.padStart(2, '0')}"
                    val finalDilution = "1:${dilutionPart.ifEmpty { "0" }}"
                    onConfirm(
                        filmType,
                        isPushPull,
                        if (isPushPull) pushPullValue else null,
                        developer,
                        temperature.toDoubleOrNull() ?: 20.0,
                        finalTime,
                        finalDilution,
                        processDescription,
                        comments
                    )
                }
            ) {
                Text(if (initialProcess == null) "Dodaj" else "Zapisz", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
