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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

enum class ProcessingMode { EDIT, CROP }

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
    var currentMode by remember { mutableStateOf(ProcessingMode.EDIT) }
    var isNegative by remember { mutableStateOf(false) }
    var showHelper by remember { mutableStateOf(false) }
    var corners by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedProcessId by remember { mutableStateOf<Int?>(null) }

    val processes by viewModel.allProcesses.collectAsStateWithLifecycle()

    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        processedBitmap = loadBitmapFromUri(context, imageUri)
    }

    val helperBitmap = remember(processedBitmap, showHelper) {
        if (showHelper && processedBitmap != null) {
            runOpenCVHelper(processedBitmap!!)
        } else null
    }

    val displayBitmap = if (showHelper && helperBitmap != null) helperBitmap else processedBitmap

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentMode == ProcessingMode.CROP) currentMode = ProcessingMode.EDIT else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
            }
            Text(
                text = if (currentMode == ProcessingMode.EDIT) "Edycja skanu" else "Kadrowanie",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń zdjęcie", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .onGloballyPositioned { containerSize = it.size }
                .pointerInput(currentMode) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (currentMode == ProcessingMode.CROP) {
                                if (corners.size < 4) corners = corners + offset else corners = listOf(offset)
                            }
                        },
                        onLongPress = {
                            if (currentMode == ProcessingMode.EDIT) showDeleteDialog = true
                        }
                    )
                }
        ) {
            displayBitmap?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            if (currentMode == ProcessingMode.CROP) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    corners.forEach { offset -> drawCircle(color = Color.Red, radius = 10.dp.toPx(), center = offset) }
                    if (corners.size == 4) {
                        for (i in 0..3) drawLine(Color.Red, corners[i], corners[(i + 1) % 4], 2.dp.toPx())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (currentMode == ProcessingMode.EDIT) {
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
        } else {
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
    onSaveClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProcess = processes.find { it.id == selectedProcessId }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCropClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Crop, null); Spacer(Modifier.width(8.dp)); Text("Kadrowanie")
        }
        
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Row(modifier = Modifier.padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InvertColors, null); Spacer(Modifier.width(8.dp)); Text("Negatyw", fontWeight = FontWeight.Medium)
                }
                Switch(checked = isNegative, onCheckedChange = onNegativeChange)
            }
        }

        // Film Process Selection
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedProcess?.let { "${it.filmType} (${it.developer})" } ?: "Wybierz wołanie...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Podepnij pod wołanie") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                processes.forEach { process ->
                    DropdownMenuItem(
                        text = { Text("${process.filmType} (${process.developer})") },
                        onClick = {
                            onProcessSelected(process.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Zastosuj i zapisz", fontSize = 16.sp)
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
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pomoc w krawędziach", fontWeight = FontWeight.Medium); Switch(checked = showHelper, onCheckedChange = onHelperChange)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onRotateLeft) { Icon(Icons.Default.RotateLeft, "Obróć w lewo") }
            IconButton(onClick = onRotateRight) { Icon(Icons.Default.RotateRight, "Obróć w prawo") }
            IconButton(onClick = onFlip) { Icon(Icons.Default.Flip, "Odbij") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onResetCorners, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Resetuj") }
            Button(onClick = onConfirmCrop, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Zatwierdź") }
        }
    }
}

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
    val res = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888); Utils.matToBitmap(bin, res); return res
}

private fun applyNegativeOpenCV(bitmap: Bitmap): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val channels = mutableListOf<Mat>(); Core.split(src, channels)
    for (i in 0 until channels.size.coerceAtMost(3)) Core.bitwise_not(channels[i], channels[i])
    Core.merge(channels, src)
    val res = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888); Utils.matToBitmap(src, res); return res
}

private fun rotateBitmapOpenCV(bitmap: Bitmap, rotationCode: Int): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val dest = Mat()
    Core.rotate(src, dest, rotationCode)
    val res = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(dest, res)
    return res
}

private fun flipBitmapOpenCV(bitmap: Bitmap): Bitmap {
    val src = Mat(); Utils.bitmapToMat(bitmap, src)
    val dest = Mat()
    Core.flip(src, dest, 1) // 1 = horizontal
    val res = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(dest, res)
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
    val res = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Utils.matToBitmap(dest, res); return res
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
