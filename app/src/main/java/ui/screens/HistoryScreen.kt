package com.example.tpfinal_pablovelazquez.ui.screens

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import com.example.tpfinal_pablovelazquez.data.Alert
import com.example.tpfinal_pablovelazquez.data.AlertDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.NavController
import java.util.*
import androidx.compose.ui.res.stringResource
import com.example.tpfinal_pablovelazquez.R

@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val alertDao = AlertDatabase.getDatabase(context).alertDao()
    var alerts by remember { mutableStateOf(listOf<Alert>()) }

    LaunchedEffect(Unit) {
        alerts = alertDao.getAllAlerts()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.my_alerts),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                color = MaterialTheme.colorScheme.primary,
                thickness = 2.dp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alerts) { alert ->
                AlertItem(alert = alert, navController = navController)
            }
        }
    }

}

@Composable
fun AlertItem(alert: Alert, navController: NavController) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("Buscando dirección...") }

    LaunchedEffect(alert.latitude, alert.longitude) {
        address = getAddressFromCoordinates(context, alert.latitude, alert.longitude)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clickable { navController.navigate("alert_detail/${alert.id}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = alert.photoPath,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alerta #${alert.id}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

suspend fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val result = geocoder.getFromLocation(latitude, longitude, 1)
            if (!result.isNullOrEmpty()) {
                val addr = result[0]
                listOfNotNull(
                    addr.thoroughfare,
                    addr.subThoroughfare,
                    addr.locality,
                    addr.adminArea
                ).joinToString(", ")
            } else {
                "Dirección no encontrada"
            }
        } catch (e: Exception) {
            "Error obteniendo dirección"
        }
    }
}
