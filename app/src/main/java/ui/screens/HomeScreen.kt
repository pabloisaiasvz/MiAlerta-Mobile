package com.example.tpfinal_pablovelazquez.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation.NavController
import com.example.tpfinal_pablovelazquez.data.Alert
import com.example.tpfinal_pablovelazquez.data.AlertDatabase
import com.example.tpfinal_pablovelazquez.data.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.ui.res.stringResource
import com.example.tpfinal_pablovelazquez.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var userHash by remember { mutableStateOf("anon_user") }

    LaunchedEffect(Unit) {
        UserManager.createOrGetUser(context) { hash ->
            userHash = hash
        }
    }

    val startAlertProcess = {
        coroutineScope.launch {
            isLoading = true
            try {
                val location = getCurrentLocation(context)
                val photoUri = takePhoto(context)
                val timestamp = System.currentTimeMillis()
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

                val alert = Alert(
                    timestamp = timestamp,
                    date = date,
                    latitude = location.first,
                    longitude = location.second,
                    location = "Ubicación registrada automáticamente",
                    photoPath = photoUri.toString()
                )

                val db = AlertDatabase.getDatabase(context)
                db.alertDao().insert(alert)

                val alertData = hashMapOf(
                    "userHash" to userHash,
                    "timestamp" to timestamp,
                    "date" to date,
                    "latitude" to location.first,
                    "longitude" to location.second,
                    "location" to "Ubicación registrada automáticamente",
                    "photoPath" to photoUri.toString()
                )

                firestore.collection("alerts").add(alertData)

                showSuccessNotification(context)
                Toast.makeText(context, "Alerta guardada correctamente", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.CAMERA, false) &&
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        ) {
            startAlertProcess()
        } else {
            Toast.makeText(context, "Los permisos de cámara y ubicación son necesarios.", Toast.LENGTH_LONG).show()
        }
    }

    val onPanicButtonClick: () -> Unit = {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            startAlertProcess()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onPanicButtonClick,
                        modifier = Modifier.size(260.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.send_alert),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): Pair<Double, Double> {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val location = fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).await()
    return Pair(location.latitude, location.longitude)
}

private suspend fun takePhoto(context: Context): Uri {
    val cameraProvider = suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        }
    }

    val imageCapture = ImageCapture.Builder().build()

    val fakeLifecycleOwner = object : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        init { lifecycleRegistry.currentState = Lifecycle.State.CREATED }
        fun doResume() { lifecycleRegistry.currentState = Lifecycle.State.RESUMED }
        fun doDestroy() { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED }
        override val lifecycle: Lifecycle get() = lifecycleRegistry
    }

    return try {
        suspendCoroutine { continuation ->
            fakeLifecycleOwner.doResume()
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture)

            val outputDirectory = context.filesDir
            val photoFile = File(
                outputDirectory,
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
            )
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        continuation.resume(savedUri)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        continuation.resume(Uri.EMPTY)
                    }
                }
            )
        }
    } finally {
        cameraProvider.unbindAll()
        fakeLifecycleOwner.doDestroy()
    }
}

private fun showSuccessNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "alert_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId, "Alertas de Pánico", android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notificaciones para alertas de pánico enviadas." }
        notificationManager.createNotificationChannel(channel)
    }

    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setContentTitle("Alerta Enviada")
        .setContentText("Tu alerta de pánico ha sido registrada correctamente.")
        .setSmallIcon(com.example.tpfinal_pablovelazquez.R.drawable.ic_launcher_foreground)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) {
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
