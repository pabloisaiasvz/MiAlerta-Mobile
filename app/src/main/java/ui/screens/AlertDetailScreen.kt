package com.example.tpfinal_pablovelazquez.ui.screens

import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.tpfinal_pablovelazquez.data.Alert
import com.example.tpfinal_pablovelazquez.data.AlertDatabase
import com.example.tpfinal_pablovelazquez.data.AlertNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import androidx.compose.ui.res.stringResource
import com.example.tpfinal_pablovelazquez.R

@Composable
fun AlertDetailScreen(alert: Alert? = null) {
    val actualAlert = alert ?: AlertNavigation.currentAlert

    if (actualAlert == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Error: No se pudo cargar la alerta")
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var address by remember { mutableStateOf("Buscando dirección...") }
    var commentText by remember { mutableStateOf(actualAlert.comment) }
    var isEditingComment by remember { mutableStateOf(false) }

    LaunchedEffect(actualAlert.latitude, actualAlert.longitude) {
        address = withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val result = geocoder.getFromLocation(actualAlert.latitude, actualAlert.longitude, 1)
                if (result?.isNotEmpty() == true) {
                    val addr = result[0]
                    listOfNotNull(
                        addr.thoroughfare,
                        addr.subThoroughfare,
                        addr.locality,
                        addr.adminArea,
                        addr.countryName
                    ).joinToString(", ")
                } else {
                    "Dirección no encontrada (${actualAlert.latitude.format(4)}, ${actualAlert.longitude.format(4)})"
                }
            } catch (e: Exception) {
                Log.e("AlertDetailScreen", "Error Geocoder", e)
                "Error obteniendo dirección"
            }
        }
    }

    val mapUrl = "https://static-maps.yandex.ru/1.x/" +
            "?ll=${actualAlert.longitude},${actualAlert.latitude}" +
            "&size=600,300" +
            "&z=15" +
            "&l=map" +
            "&pt=${actualAlert.longitude},${actualAlert.latitude},pm2rdm" +
            "&lang=es_ES"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.alert_details),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary,
            thickness = 2.dp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Fecha: ${actualAlert.date}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ubicación: $address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.incident_map),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            val mapPainter = rememberAsyncImagePainter(mapUrl)
            val mapState = mapPainter.state
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = mapPainter,
                    contentDescription = "Mapa del incidente",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                when (mapState) {
                    is AsyncImagePainter.State.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is AsyncImagePainter.State.Error -> {
                        Text(
                            "Error al cargar mapa",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(4.dp),
                            color = Color.White
                        )
                        Log.e("AlertDetailScreen", "Error Coil Mapa: ${mapState.result.throwable}")
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.incident_photo),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            val photoPainter = rememberAsyncImagePainter(model = Uri.parse(actualAlert.photoPath))
            val photoState = photoPainter.state
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = photoPainter,
                    contentDescription = "Foto registrada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                when (photoState) {
                    is AsyncImagePainter.State.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is AsyncImagePainter.State.Error -> {
                        Text(
                            "Error al cargar foto",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(4.dp),
                            color = Color.White
                        )
                        Log.e("AlertDetailScreen", "Error Coil Foto: ${photoState.result.throwable}")
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.commentary),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    IconButton(
                        onClick = { isEditingComment = !isEditingComment }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar comentario",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditingComment) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe un comentario...") },
                        minLines = 3,
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            commentText = actualAlert.comment
                            isEditingComment = false
                        }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val updatedAlert = actualAlert.copy(comment = commentText)
                                    AlertDatabase.getDatabase(context).alertDao().update(updatedAlert)
                                    if (AlertNavigation.currentAlert?.id == actualAlert.id) {
                                        AlertNavigation.currentAlert = updatedAlert
                                    }
                                }
                            }
                            isEditingComment = false
                        }) {
                            Text("Guardar")
                        }
                    }
                } else {
                    Text(
                        text = if (commentText.isNotBlank()) commentText else "Sin comentarios aún",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (commentText.isNotBlank())
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)