package com.example.tpfinal_pablovelazquez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tpfinal_pablovelazquez.data.Alert
import com.example.tpfinal_pablovelazquez.data.AlertDatabase
import com.example.tpfinal_pablovelazquez.data.AlertNavigation
import com.example.tpfinal_pablovelazquez.data.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.example.tpfinal_pablovelazquez.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    userHash: String,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = if (userHash != "anon_user") "home" else "login"

    var showMenu by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            if (currentRoute != "login") {
                TopBar(
                    navController = navController,
                    showMenu = showMenu,
                    onMenuClick = { showMenu = it },
                    userHash = userHash,
                    onLogout = {
                        onLogout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") { LoginScreen(navController) }
            composable("home") { HomeScreen(navController) }
            composable("history") { HistoryScreen(navController) }

            composable("alert_detail/{alertId}") { backStackEntry ->
                val alertIdStr = backStackEntry.arguments?.getString("alertId")
                val alertId = alertIdStr?.toLongOrNull()
                var alert by remember { mutableStateOf<Alert?>(null) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(alertId) {
                    if (alertId != null) {
                        scope.launch {
                            alert = withContext(Dispatchers.IO) {
                                AlertDatabase.getDatabase(context).alertDao().getAlertById(alertId)
                            }
                        }
                    }
                }
                alert?.let { AlertDetailScreen(alert = it) } ?: run {
                    Text("Cargando detalles...")
                }
            }

            composable("alert_detail") {
                val alert = AlertNavigation.currentAlert
                if (alert != null) {
                    AlertDetailScreen(alert = alert)
                } else {
                    Text("Error: No se pudo cargar la alerta")
                }
            }

            composable("subscribed_alert_detail") {
                val subscribedAlert = AlertNavigation.currentSubscribedAlert
                if (subscribedAlert != null) {
                    val alert = Alert(
                        id = 0,
                        timestamp = subscribedAlert.timestamp ?: System.currentTimeMillis(),
                        date = subscribedAlert.date ?: "",
                        latitude = subscribedAlert.latitude ?: 0.0,
                        longitude = subscribedAlert.longitude ?: 0.0,
                        location = subscribedAlert.location ?: "",
                        photoPath = subscribedAlert.photoPath ?: ""
                    )
                    AlertDetailScreen(alert = alert)
                } else {
                    Text("Error: No se pudo cargar la alerta")
                }
            }

            composable("settings") {
                SettingsScreen(
                    navController = navController,
                    userHash = userHash,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    language = language,
                    onLanguageChange = onLanguageChange
                )
            }

            composable("subscribed_alerts") { SubscribedAlertsScreen(navController) }
            composable("subscribed_management") {
                SubscribedManagementScreen(navController, userHash = userHash)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavController,
    showMenu: Boolean,
    onMenuClick: (Boolean) -> Unit,
    userHash: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = "MiAlerta",
                fontSize = 22.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clickable {
                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                }
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
        actions = {
            IconButton(onClick = { navController.navigate("history") }) {
                Icon(Icons.Filled.History, contentDescription = "Historial", tint = MaterialTheme.colorScheme.onPrimary)
            }
            IconButton(onClick = { navController.navigate("subscribed_alerts") }) {
                Icon(Icons.Filled.Notifications, contentDescription = "Alertas suscritas", tint = MaterialTheme.colorScheme.onPrimary)
            }
            IconButton(onClick = { onMenuClick(true) }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menú", tint = MaterialTheme.colorScheme.onPrimary)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { onMenuClick(false) }) {
                DropdownMenuItem(text = { Text(text = stringResource(R.string.settings)) }, onClick = {
                    onMenuClick(false)
                    navController.navigate("settings")
                })
                DropdownMenuItem(text = { Text(text = stringResource(R.string.logout)) }, onClick = {
                    onMenuClick(false)
                    UserManager.resetUser(context) {
                        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                        onLogout()
                    }
                })
            }
        }
    )
}