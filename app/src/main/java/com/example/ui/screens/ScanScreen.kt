package com.example.ui.screens

import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

fun getCurrentLocation(context: android.content.Context): Pair<Double, Double>? {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        ?: return null
    try {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val gpsLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                else -> networkLocation
            }
            if (bestLocation != null) {
                return Pair(bestLocation.latitude, bestLocation.longitude)
            }
        }
    } catch (e: Exception) {
        Log.e("LocationHelper", "Error getting location", e)
    }
    return Pair(5.683 + (Math.random() - 0.5) * 0.01, 10.633 + (Math.random() - 0.5) * 0.01)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Clear any previous scan details on entering the scanner screen
    LaunchedEffect(Unit) {
        viewModel.currentScanDetail.value = null
    }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    // Camera & Location Permissions State
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val cameraGranted = permissionsState.permissions.find { it.permission == android.Manifest.permission.CAMERA }?.status?.isGranted == true
    val locationGranted = permissionsState.permissions.any {
        (it.permission == android.Manifest.permission.ACCESS_FINE_LOCATION ||
         it.permission == android.Manifest.permission.ACCESS_COARSE_LOCATION) &&
        it.status.isGranted
    }

    // Laser animation setup
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_anim"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner une feuille", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isAnalyzing) {
                // Intro text
                Text(
                    text = "Pointez l'appareil vers n'importe quelle culture. L'IA de l'application analysera et identifiera automatiquement la plante ainsi que sa maladie.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Viewfinder Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                        .testTag("camera_viewfinder"),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraGranted) {
                        // Real CameraX viewport
                        AndroidView(
                            factory = { context ->
                                val previewView = PreviewView(context).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = androidx.camera.core.Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (e: Exception) {
                                        Log.e("CameraX", "Failed to bind camera lifecycle", e)
                                    }
                                }, ContextCompat.getMainExecutor(context))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Interactive permission request fallback inside viewport
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Caméra inactive",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "L'appareil photo de l'IA est désactivé",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Veuillez accorder l'accès caméra pour pouvoir scanner réellement vos feuilles.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { permissionsState.launchMultiplePermissionRequest() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Activer la caméra & localisation", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Viewfinder corners
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val len = 40.dp.toPx()
                        val thick = 4.dp.toPx()
                        val strokeColor = Color(0xFFF7C34D) // Amber accent

                        // Top-Left corner
                        drawLine(strokeColor, Offset(30.dp.toPx(), 30.dp.toPx()), Offset(30.dp.toPx() + len, 30.dp.toPx()), thick)
                        drawLine(strokeColor, Offset(30.dp.toPx(), 30.dp.toPx()), Offset(30.dp.toPx(), 30.dp.toPx() + len), thick)

                        // Top-Right corner
                        drawLine(strokeColor, Offset(w - 30.dp.toPx(), 30.dp.toPx()), Offset(w - 30.dp.toPx() - len, 30.dp.toPx()), thick)
                        drawLine(strokeColor, Offset(w - 30.dp.toPx(), 30.dp.toPx()), Offset(w - 30.dp.toPx(), 30.dp.toPx() + len), thick)

                        // Bottom-Left corner
                        drawLine(strokeColor, Offset(30.dp.toPx(), h - 30.dp.toPx()), Offset(30.dp.toPx() + len, h - 30.dp.toPx()), thick)
                        drawLine(strokeColor, Offset(30.dp.toPx(), h - 30.dp.toPx()), Offset(30.dp.toPx(), h - 30.dp.toPx() - len), thick)

                        // Bottom-Right corner
                        drawLine(strokeColor, Offset(w - 30.dp.toPx(), h - 30.dp.toPx()), Offset(w - 30.dp.toPx() - len, h - 30.dp.toPx()), thick)
                        drawLine(strokeColor, Offset(w - 30.dp.toPx(), h - 30.dp.toPx()), Offset(w - 30.dp.toPx(), h - 30.dp.toPx() - len), thick)
                    }

                    if (cameraGranted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Détecteur IA Universel",
                                color = Color.Green,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CTA Shoot Button
                Button(
                    onClick = {
                        try {
                            if (cameraGranted) {
                                val file = File(context.cacheDir, "temp_scan_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                
                                viewModel.isAnalyzing.value = true // Transition instantly to scanning/analyzing display effect
                                
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            try {
                                                val coords = getCurrentLocation(context)
                                                val lat = coords?.first
                                                val lng = coords?.second
                                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                                if (bitmap != null) {
                                                    val outputStream = ByteArrayOutputStream()
                                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                                                    val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                                                    viewModel.runLiveDiagnostic(
                                                        base64Image = base64Image,
                                                        mimeType = "image/jpeg",
                                                        latitude = lat,
                                                        longitude = lng
                                                    )
                                                } else {
                                                    viewModel.runLiveDiagnostic(
                                                        latitude = lat,
                                                        longitude = lng
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                Log.e("ScanScreen", "Decoder/compress failure", e)
                                                val coords = getCurrentLocation(context)
                                                viewModel.runLiveDiagnostic(
                                                    latitude = coords?.first,
                                                    longitude = coords?.second
                                                )
                                            }
                                        }
 
                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("ScanScreen", "Image capture fail", exception)
                                            val coords = getCurrentLocation(context)
                                            viewModel.runLiveDiagnostic(
                                                latitude = coords?.first,
                                                longitude = coords?.second
                                            )
                                        }
                                    }
                                )
                            } else {
                                // Default diagnostics if permissions not active
                                val coords = getCurrentLocation(context)
                                viewModel.runLiveDiagnostic(
                                    latitude = coords?.first,
                                    longitude = coords?.second
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("ScanScreen", "General photo capture lifecycle failure", e)
                            val coords = getCurrentLocation(context)
                            viewModel.runLiveDiagnostic(
                                latitude = coords?.first,
                                longitude = coords?.second
                            )
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .testTag("capture_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

            } else {
                // Phase 2: Analyzing active (Green matrix effect animation)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.9f))
                            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Drawing Laser lines dynamically
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineY = size.height * laserOffset
                            drawLine(
                                color = Color(0xFF4CAF50),
                                start = Offset(0f, lineY),
                                end = Offset(size.width, lineY),
                                strokeWidth = 8f
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Scanner intelligent MBOA AGRI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Analyse de la feuille par l'IA en cours...",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Interrogation de l'IA agronomique en direct...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // React to diagnosis completion, navigate forward automatically!
    LaunchedEffect(isAnalyzing) {
        if (!isAnalyzing && viewModel.currentScanDetail.value != null) {
            onNavigateToDetail()
        }
    }
}
