package ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tpfinal_pablovelazquez.data.AlertNavigation
import com.google.gson.Gson
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.tpfinal_pablovelazquez.data.Alert

@Composable
fun AlertItem(alert: Alert, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                AlertNavigation.currentAlert = alert
                navController.navigate("alert_detail")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${alert.date}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${alert.location ?: "Ubicación desconocida"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}