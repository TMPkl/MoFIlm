package com.example.mofilm.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mofilm.data.FilmProcess
import com.example.mofilm.ui.viewmodels.FilmProcessViewModel
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt
import org.opencv.core.Point as CvPoint

enum class ProcessingMode { EDIT, CROP, COLOR }

@Composable
fun ScannerScreen(viewModel: FilmProcessViewModel = viewModel()) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var galleryImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mofilmScans by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val refreshLists = {
        galleryImages = fetchImages(context, FetchMode.GENERAL)
        mofilmScans = fetchImages(context, FetchMode.MOFILM_SCANS)
    }

    if (isProcessing && capturedImageUri != null) {
        ProcessingView(
            imageUri = capturedImageUri!!,
            viewModel = viewModel,
            onBack = { 
                isProcessing = false
                refreshLists()
            }
        )
    } else {
        ScannerMenuView(
            capturedImageUri = capturedImageUri,
            galleryImages = galleryImages,
            mofilmScans = mofilmScans,
            onRefresh = refreshLists,
            onImageCaptured = { uri ->
                capturedImageUri = uri
                isProcessing = true
            }
        )
    }
}

@Composable
fun ScannerMenuView(
    capturedImageUri: Uri?,
    galleryImages: List<Uri>,
    mofilmScans: List<Uri>,
    onRefresh: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var imageToDelete by remember { mutableStateOf<Uri?>(null) }
    
    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onRefresh()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentUri?.let { onImageCaptured(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = createImageUri(context)
                currentUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, galleryPermission) == PackageManager.PERMISSION_GRANTED) {
            onRefresh()
        } else {
            galleryLauncher.launch(galleryPermission)
        }
    }

    if (imageToDelete != null) {
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text("Usuń zdjęcie") },
            text = { Text("Czy na pewno chcesz usunąć to zdjęcie z galerii?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteImage(context, imageToDelete!!)
                    imageToDelete = null
                    onRefresh()
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) { Text("Anuluj") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Film Scanner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val uri = createImageUri(context)
                        currentUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nowy skan")
            }

            OutlinedButton(
                onClick = { /* Picker logic if needed */ },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Z galerii")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (mofilmScans.isNotEmpty()) {
            Text(
                text = "Moje skany",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(mofilmScans) { uri ->
                    GalleryImageItem(
                        uri = uri,
                        onClick = { onImageCaptured(uri) },
                        onLongClick = { imageToDelete = uri }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        Text(
            text = "Ostatnie ujęcia z telefonu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (galleryImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Brak zdjęć", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(galleryImages) { uri ->
                    GalleryImageItem(
                        uri = uri,
                        onClick = { onImageCaptured(uri) },
                        onLongClick = { imageToDelete = uri }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ProcessingView(imageUri: Uri, viewModel: FilmProcessViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    var currentMode by remember { mutableStateOf(ProcessingMode.EDIT) }
    var isNegative by remember { mutableStateOf(false) }
    var showHelper by remember { mutableStateOf(false) }
    var corners by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedProcessId by remember { mutableStateOf<Int?>(null) }

    // Color Correction states
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var temp by remember { mutableFloatStateOf(0f) }
    var tint by remember { mutableFloatStateOf(0f) }
    var ev by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var shadows by remember { mutableFloatStateOf(0f) }
    var isPickingWB by remember { mutableStateOf(false) }

    val processes by viewModel.allProcesses.collectAsStateWithLifecycle()

    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        processedBitmap = loadBitmapFromUri(context, imageUri)
    }

    val displayBitmap = remember(processedBitmap, showHelper, contrast, saturation, temp, tint, ev, brightness, shadows, currentMode) {
        if (processedBitmap == null) return@remember null
        
        if (showHelper) {
            runOpenCVHelper(processedBitmap!!)
        } else if (currentMode == ProcessingMode.COLOR) {
            applyColorAdjustments(processedBitmap!!, contrast, saturation, temp, tint, ev, brightness, shadows)
        } else {
            processedBitmap
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usuń zdjęcie") },
            text = { Text("Czy na pewno chcesz usunąć to zdjęcie z galerii?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteImage(context, imageUri)
                    showDeleteDialog = false
                    onBack()
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Anuluj") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp, 4.dp, 12.dp, 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentMode != ProcessingMode.EDIT) currentMode = ProcessingMode.EDIT else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć", Modifier.size(20.dp))
            }
            Text(
                text = when(currentMode) {
                    ProcessingMode.EDIT -> "Edycja"
                    ProcessingMode.CROP -> "Kadrowanie"
                    ProcessingMode.COLOR -> "Kolory"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .onGloballyPositioned { containerSize = it.size }
                .pointerInput(currentMode, isPickingWB) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (currentMode == ProcessingMode.CROP) {
                                if (corners.size < 4) corners = corners + offset else corners = listOf(offset)
                            } else if (currentMode == ProcessingMode.COLOR && isPickingWB && processedBitmap != null) {
                                val coords = mapOffsetToBitmap(offset, containerSize, processedBitmap!!.width, processedBitmap!!.height)
                                if (coords != null) {
                                    processedBitmap = applyWhiteBalancePoint(processedBitmap!!, coords.first, coords.second)
                                    isPickingWB = false
                                }
                            }
                        },
                        onLongPress = {
                            if (currentMode == ProcessingMode.EDIT) showDeleteDialog = true
                        }
                    )
                }
        ) {
            displayBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            if (isPickingWB) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Wskaż neutralny punkt",
                        color = Color.White,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(12.dp, 6.dp),
                        fontSize = 11.sp
                    )
                }
            }

            if (currentMode == ProcessingMode.CROP) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    corners.forEach { offset -> drawCircle(color = Color.Red, radius = 6.dp.toPx(), center = offset) }
                    if (corners.size == 4) {
                        for (i in 0..3) drawLine(Color.Red, corners[i], corners[(i + 1) % 4], 2.dp.toPx())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.heightIn(max = screenHeight * 0.22f).fillMaxWidth()) {
            when (currentMode) {
                ProcessingMode.EDIT -> {
                    EditMenu(
                        isNegative = isNegative,
                        processes = processes,
                        selectedProcessId = selectedProcessId,
                        onProcessSelected = { selectedProcessId = it },
                        onNegativeChange = { checked ->
                            isNegative = checked
                            processedBitmap?.let { processedBitmap = applyNegativeOpenCV(it) }
                        },
                        onCropClick = { currentMode = ProcessingMode.CROP },
                        onColorClick = { currentMode = ProcessingMode.COLOR },
                        onSaveClick = {
                            processedBitmap?.let { bitmap ->
                                val savedUri = saveBitmapToGallery(context, bitmap)
                                if (savedUri != null && selectedProcessId != null) {
                                    viewModel.addScanToProcess(selectedProcessId!!, savedUri.toString())
                                }
                                Toast.makeText(context, "Zapisano!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    )
                }
                ProcessingMode.CROP -> {
                    CropMenu(
                        showHelper = showHelper,
                        onHelperChange = { showHelper = it },
                        onRotateLeft = {
                            processedBitmap?.let {
                                processedBitmap = rotateBitmapOpenCV(it, Core.ROTATE_90_COUNTERCLOCKWISE)
                                corners = emptyList()
                            }
                        },
                        onRotateRight = {
                            processedBitmap?.let {
                                processedBitmap = rotateBitmapOpenCV(it, Core.ROTATE_90_CLOCKWISE)
                                corners = emptyList()
                            }
                        },
                        onFlip = {
                            processedBitmap?.let {
                                processedBitmap = flipBitmapOpenCV(it)
                                corners = emptyList()
                            }
                        },
                        onResetCorners = { corners = emptyList() },
                        onConfirmCrop = {
                            if (corners.size == 4 && processedBitmap != null && containerSize != IntSize.Zero) {
                                try {
                                    processedBitmap = perspectiveWarp(processedBitmap!!, corners, containerSize)
                                    corners = emptyList()
                                    currentMode = ProcessingMode.EDIT
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Zaznacz 4 narożniki", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                ProcessingMode.COLOR -> {
                    ColorMenu(
                        contrast = contrast,
                        onContrastChange = { contrast = it },
                        saturation = saturation,
                        onSaturationChange = { saturation = it },
                        temp = temp,
                        onTempChange = { temp = it },
                        tint = tint,
                        onTintChange = { tint = it },
                        ev = ev,
                        onEvChange = { ev = it },
                        brightness = brightness,
                        onBrightnessChange = { brightness = it },
                        shadows = shadows,
                        onShadowsChange = { shadows = it },
                        isPickingWB = isPickingWB,
                        onPickWBClick = { isPickingWB = !isPickingWB },
                        onReset = {
                            contrast = 1f
                            saturation = 1f
                            temp = 0f
                            tint = 0f
                            ev = 0f
                            brightness = 0f
                            shadows = 0f
                            isPickingWB = false
                        },
                        onConfirm = {
                            processedBitmap = applyColorAdjustments(processedBitmap!!, contrast, saturation, temp, tint, ev, brightness, shadows)
                            contrast = 1f
                            saturation = 1f
                            temp = 0f
                            tint = 0f
                            ev = 0f
                            brightness = 0f
                            shadows = 0f
                            currentMode = ProcessingMode.EDIT
                        },
                        onBack = { currentMode = ProcessingMode.EDIT }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMenu(
    isNegative: Boolean,
    processes: List<FilmProcess>,
    selectedProcessId: Int?,
    onProcessSelected: (Int?) -> Unit,
    onNegativeChange: (Boolean) -> Unit,
    onCropClick: () -> Unit,
    onColorClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProcess = processes.find { it.id == selectedProcessId }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCropClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.Crop, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Kadruj", fontSize = 12.sp)
            }
            
            OutlinedButton(onClick = onColorClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.ColorLens, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Kolory", fontSize = 12.sp)
            }
        }
        
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Row(modifier = Modifier.padding(12.dp, 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InvertColors, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Negatyw", fontSize = 13.sp)
                }
                Switch(checked = isNegative, onCheckedChange = onNegativeChange, modifier = Modifier.scale(0.7f))
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedProcess?.let { "${it.filmType} (${it.developer})" } ?: "Wybierz wołanie...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Wołanie", fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                processes.forEach { process ->
                    DropdownMenuItem(
                        text = { Text("${process.filmType} (${process.developer})", fontSize = 13.sp) },
                        onClick = {
                            onProcessSelected(process.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Zapisz skan", fontSize = 14.sp)
        }
    }
}

@Composable
fun CropMenu(
    showHelper: Boolean,
    onHelperChange: (Boolean) -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlip: () -> Unit,
    onResetCorners: () -> Unit,
    onConfirmCrop: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pomoc w krawędziach", fontSize = 13.sp); Switch(checked = showHelper, onCheckedChange = onHelperChange, modifier = Modifier.scale(0.7f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onRotateLeft) { Icon(Icons.Default.RotateLeft, "Obróć", Modifier.size(20.dp)) }
            IconButton(onClick = onRotateRight) { Icon(Icons.Default.RotateRight, "Obróć", Modifier.size(20.dp)) }
            IconButton(onClick = onFlip) { Icon(Icons.Default.Flip, "Odbij", Modifier.size(20.dp)) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onResetCorners, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Reset", fontSize = 13.sp) }
            Button(onClick = onConfirmCrop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Zatwierdź", fontSize = 13.sp) }
        }
    }
}

@Composable
fun ColorMenu(
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    temp: Float,
    onTempChange: (Float) -> Unit,
    tint: Float,
    onTintChange: (Float) -> Unit,
    ev: Float,
    onEvChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    shadows: Float,
    onShadowsChange: (Float) -> Unit,
    isPickingWB: Boolean,
    onPickWBClick: () -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Korekcja obrazu", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            TextButton(onClick = onReset) { Text("Reset", fontSize = 12.sp) }
        }

        ColorSliderItem("Ekspozycja (EV)", ev, -3f..3f, onEvChange, "%.1f")
        ColorSliderItem("Jasność", brightness, -100f..100f, onBrightnessChange, "%.0f")
        ColorSliderItem("Kontrast", contrast, 0.5f..2.0f, onContrastChange, "%.1f")
        ColorSliderItem("Cienie", shadows, -100f..100f, onShadowsChange, "%.0f")
        ColorSliderItem("Nasycenie", saturation, 0.0f..2.0f, onSaturationChange, "%.1f")
        ColorSliderItem("Temp (Ciepło-Zimno)", temp, -100f..100f, onTempChange, "%.0f")
        ColorSliderItem("Odcień (Ziel-Mag)", tint, -100f..100f, onTintChange, "%.0f")

        OutlinedButton(
            onClick = onPickWBClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = if (isPickingWB) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
            contentPadding = PaddingValues(4.dp)
        ) {
            Icon(Icons.Default.Colorize, null, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isPickingWB) "Wskaż punkt..." else "Wybierz punkt WB", fontSize = 12.sp)
        }

        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Zastosuj", fontSize = 14.sp)
        }
        
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ColorSliderItem(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, format: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(32.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.45f), fontSize = 10.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(0.4f).height(32.dp)
        )
        Text(format.format(value), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.15f), textAlign = TextAlign.End, fontSize = 10.sp)
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    try { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "MoFilm_Scan_$timeStamp.jpg"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MoFilm/Scans")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
    }
    return uri
}

private fun deleteImage(context: Context, uri: Uri) {
    try {
        context.contentResolver.delete(uri, null, null)
        Toast.makeText(context, "Usunięto zdjęcie", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Błąd usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun runOpenCVHelper(bitmap: Bitmap): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val gray = Mat(); Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY)
    val small = Mat(); Imgproc.resize(gray, small, Size(gray.cols() / 4.0, gray.rows() / 4.0))
    val samples = small.reshape(1, small.rows() * small.cols()); samples.convertTo(samples, CvType.CV_32F)
    val centers = Mat()
    Core.kmeans(samples, 3, Mat(), TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0), 10, Core.KMEANS_RANDOM_CENTERS, centers)
    val clusterVals = mutableListOf<Double>(); for (i in 0 until 3) clusterVals.add(centers.get(i, 0)[0]); clusterVals.sort()
    val threshold = clusterVals[1] + (clusterVals[2] - clusterVals[1]) / 2.0
    val bin = Mat(); Imgproc.threshold(gray, bin, threshold, 255.0, Imgproc.THRESH_BINARY)
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    Imgproc.erode(bin, bin, kernel, CvPoint(-1.0, -1.0), 3); Imgproc.dilate(bin, bin, kernel, CvPoint(-1.0, -1.0), 3)
    val res = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888); Utils.matToBitmap(bin, res)
    src.release(); gray.release(); small.release(); samples.release(); centers.release(); bin.release(); kernel.release()
    return res
}

private fun applyNegativeOpenCV(bitmap: Bitmap): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val channels = mutableListOf<Mat>(); Core.split(src, channels)
    for (i in 0 until channels.size.coerceAtMost(3)) Core.bitwise_not(channels[i], channels[i])
    Core.merge(channels, src)
    val res = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888); Utils.matToBitmap(src, res)
    src.release(); channels.forEach { it.release() }
    return res
}

private fun rotateBitmapOpenCV(bitmap: Bitmap, rotationCode: Int): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val dest = Mat()
    Core.rotate(src, dest, rotationCode)
    val res = Bitmap.createBitmap(dest.cols(), dest.rows(), bitmap.config ?: Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(dest, res)
    src.release(); dest.release()
    return res
}

private fun flipBitmapOpenCV(bitmap: Bitmap): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val dest = Mat()
    Core.flip(src, dest, 1) // 1 = horizontal
    val res = Bitmap.createBitmap(dest.cols(), dest.rows(), bitmap.config ?: Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(dest, res)
    src.release(); dest.release()
    return res
}

private fun perspectiveWarp(bitmap: Bitmap, corners: List<Offset>, containerSize: IntSize): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val scale = minOf(containerSize.width.toFloat() / bitmap.width, containerSize.height.toFloat() / bitmap.height)
    val ox = (containerSize.width - bitmap.width * scale) / 2f
    val oy = (containerSize.height - bitmap.height * scale) / 2f
    val srcPoints = corners.map { CvPoint(((it.x - ox) / scale).toDouble(), ((it.y - oy) / scale).toDouble()) }
    val sorted = sortPointsForWarp(srcPoints)
    val w = sqrt((sorted[2].x - sorted[3].x).pow(2.0) + (sorted[2].y - sorted[3].y).pow(2.0)).coerceAtLeast(sqrt((sorted[1].x - sorted[0].x).pow(2.0) + (sorted[1].y - sorted[0].y).pow(2.0))).toInt()
    val h = sqrt((sorted[1].x - sorted[2].x).pow(2.0) + (sorted[1].y - sorted[2].y).pow(2.0)).coerceAtLeast(sqrt((sorted[0].x - sorted[3].x).pow(2.0) + (sorted[0].y - sorted[3].y).pow(2.0))).toInt()
    val warpMat = Imgproc.getPerspectiveTransform(Converters.vector_Point2f_to_Mat(sorted), Converters.vector_Point2f_to_Mat(listOf(CvPoint(0.0, 0.0), CvPoint(w.toDouble(), 0.0), CvPoint(w.toDouble(), h.toDouble()), CvPoint(0.0, h.toDouble()))))
    val dest = Mat(); Imgproc.warpPerspective(src, dest, warpMat, Size(w.toDouble(), h.toDouble()))
    val res = Bitmap.createBitmap(w, h, bitmap.config ?: Bitmap.Config.ARGB_8888); Utils.matToBitmap(dest, res)
    src.release(); dest.release(); warpMat.release()
    return res
}

private fun applyColorAdjustments(
    bitmap: Bitmap,
    contrast: Float,
    saturation: Float,
    temp: Float,
    tint: Float,
    ev: Float,
    brightness: Float,
    shadows: Float
): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val alpha = contrast * 2.0.pow(ev.toDouble())
    val beta = brightness.toDouble()
    src.convertTo(src, -1, alpha, beta)
    if (shadows != 0f) {
        val lut = Mat(1, 256, CvType.CV_8U)
        val lutData = ByteArray(256)
        for (i in 0..255) {
            var v = i.toDouble()
            if (i < 128) {
                val mask = (128.0 - i) / 128.0
                v += shadows.toDouble() * mask
            }
            lutData[i] = v.coerceIn(0.0, 255.0).toInt().toByte()
        }
        lut.put(0, 0, lutData)
        val channels = mutableListOf<Mat>(); Core.split(src, channels)
        for (j in 0 until channels.size.coerceAtMost(3)) Core.LUT(channels[j], lut, channels[j])
        Core.merge(channels, src)
        lut.release(); channels.forEach { it.release() }
    }
    if (saturation != 1.0f) {
        val hsv = Mat(); val rgb = Mat()
        Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV)
        val channels = mutableListOf<Mat>(); Core.split(hsv, channels)
        channels[1].convertTo(channels[1], -1, saturation.toDouble(), 0.0)
        Core.merge(channels, hsv)
        Imgproc.cvtColor(hsv, rgb, Imgproc.COLOR_HSV2RGB)
        Imgproc.cvtColor(rgb, src, Imgproc.COLOR_RGB2RGBA)
        rgb.release(); hsv.release(); channels.forEach { it.release() }
    }
    if (temp != 0f || tint != 0f) {
        val channels = mutableListOf<Mat>(); Core.split(src, channels)
        val rGain = (1.0 + (temp / 200.0)).coerceAtLeast(0.1)
        val bGain = (1.0 - (temp / 200.0)).coerceAtLeast(0.1)
        val gGain = (1.0 + (tint / 200.0)).coerceAtLeast(0.1)
        val rbGain = (1.0 - (tint / 400.0)).coerceAtLeast(0.1)
        channels[0].convertTo(channels[0], -1, rGain * rbGain, 0.0)
        channels[1].convertTo(channels[1], -1, gGain, 0.0)
        channels[2].convertTo(channels[2], -1, bGain * rbGain, 0.0)
        Core.merge(channels, src)
        channels.forEach { it.release() }
    }
    val res = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888); Utils.matToBitmap(src, res)
    src.release()
    return res
}

private fun applyWhiteBalancePoint(bitmap: Bitmap, x: Int, y: Int): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val pixel = src.get(y, x) ?: run { src.release(); return bitmap }
    if (pixel.size < 3) { src.release(); return bitmap }
    val avg = (pixel[0] + pixel[1] + pixel[2]) / 3.0
    if (avg == 0.0) { src.release(); return bitmap }
    val channels = mutableListOf<Mat>(); Core.split(src, channels)
    channels[0].convertTo(channels[0], -1, avg / pixel[0], 0.0)
    channels[1].convertTo(channels[1], -1, avg / pixel[1], 0.0)
    channels[2].convertTo(channels[2], -1, avg / pixel[2], 0.0)
    Core.merge(channels, src)
    val res = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888); Utils.matToBitmap(src, res)
    src.release(); channels.forEach { it.release() }
    return res
}

private fun mapOffsetToBitmap(offset: Offset, containerSize: IntSize, bitmapWidth: Int, bitmapHeight: Int): Pair<Int, Int>? {
    if (containerSize.width <= 0 || containerSize.height <= 0) return null
    val scale = minOf(containerSize.width.toFloat() / bitmapWidth, containerSize.height.toFloat() / bitmapHeight)
    val dx = (containerSize.width - bitmapWidth * scale) / 2f
    val dy = (containerSize.height - bitmapHeight * scale) / 2f
    val bx = ((offset.x - dx) / scale).toInt(); val by = ((offset.y - dy) / scale).toInt()
    return if (bx in 0 until bitmapWidth && by in 0 until bitmapHeight) bx to by else null
}

private fun sortPointsForWarp(p: List<CvPoint>): List<CvPoint> {
    val sSum = p.sortedBy { it.x + it.y }; val tl = sSum.first(); val br = sSum.last()
    val sDiff = p.sortedBy { it.y - it.x }; val tr = sDiff.first(); val bl = sDiff.last(); return listOf(tl, tr, br, bl)
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryImageItem(uri: Uri, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private enum class FetchMode { GENERAL, MOFILM_SCANS }

private fun fetchImages(context: Context, mode: FetchMode): List<Uri> {
    val images = mutableListOf<Uri>()
    val pathScans = "%Pictures/MoFilm/Scans%"
    val pathMoFilmRoot = "%Pictures/MoFilm%"
    val selection = when (mode) {
        FetchMode.MOFILM_SCANS -> if (Build.VERSION.SDK_INT >= 29) "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" else "${MediaStore.MediaColumns.DATA} LIKE ?"
        FetchMode.GENERAL -> if (Build.VERSION.SDK_INT >= 29) "${MediaStore.MediaColumns.RELATIVE_PATH} NOT LIKE ?" else "${MediaStore.MediaColumns.DATA} NOT LIKE ?"
    }
    val selectionArgs = arrayOf(if (mode == FetchMode.MOFILM_SCANS) pathScans else pathMoFilmRoot)
    context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), selection, selectionArgs, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        var count = 0
        while (cursor.moveToNext() && count < 25) {
            images.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol)))
            count++
        }
    }
    return images
}

private fun createImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "MoFilm_Raw_$timeStamp.jpg"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg"); put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MoFilm/Raw")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: throw Exception("Error")
}
