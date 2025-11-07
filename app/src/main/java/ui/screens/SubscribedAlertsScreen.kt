package com.example.tpfinal_pablovelazquez.ui.screens

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.tpfinal_pablovelazquez.data.SubscribedAlert
import com.example.tpfinal_pablovelazquez.data.UserManager
import com.example.tpfinal_pablovelazquez.data.toAlert
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import com.example.tpfinal_pablovelazquez.data.AlertNavigation
import androidx.compose.ui.res.stringResource
import com.example.tpfinal_pablovelazquez.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscribedAlertsScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val currentUserHash = UserManager.getUserHash(context)
    var alerts by remember { mutableStateOf(listOf<SubscribedAlert>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            Log.d("SubscribedAlerts", "Usuario actual: $currentUserHash")

            val subscriptionsSnapshot = firestore.collection("users")
                .document(currentUserHash)
                .collection("subscriptions")
                .get()
                .await()

            val subscribedHashes = subscriptionsSnapshot.documents.mapNotNull { it.getString("hash") }
            Log.d("SubscribedAlerts", "Hashes suscritos: $subscribedHashes")

            val fetchedAlerts = mutableListOf<SubscribedAlert>()

            for (hash in subscribedHashes) {
                val alertsSnapshot = firestore.collection("alerts")
                    .whereEqualTo("userHash", hash)
                    .get()
                    .await()

                fetchedAlerts.addAll(alertsSnapshot.toObjects(SubscribedAlert::class.java))
            }

            alerts = fetchedAlerts.sortedByDescending { it.timestamp ?: 0 }
        } catch (e: Exception) {
            Log.e("SubscribedAlerts", "Error cargando alertas", e)
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.subscribed_alerts),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                color = MaterialTheme.colorScheme.primary,
                thickness = 2.dp
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            alerts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay alertas de tus suscripciones aún.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts) { alert ->
                        SubscribedAlertItem(alert = alert, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscribedAlertItem(alert: SubscribedAlert, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clickable {
                AlertNavigation.currentSubscribedAlert = alert
                navController.navigate("subscribed_alert_detail")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = alert.photoPath,
                contentDescription = "Foto de alerta",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.date ?: "Fecha desconocida",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alert.location ?: "Ubicación desconocida",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Ver detalle",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

