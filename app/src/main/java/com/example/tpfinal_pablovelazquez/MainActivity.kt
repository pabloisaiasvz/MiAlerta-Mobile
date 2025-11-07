package com.example.tpfinal_pablovelazquez

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tpfinal_pablovelazquez.data.UserManager
import com.example.tpfinal_pablovelazquez.ui.screens.AppNavigation
import com.example.tpfinal_pablovelazquez.ui.theme.TPFinalPabloVelazquezTheme
import com.example.tpfinal_pablovelazquez.utils.LocaleHelper
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LocaleHelper.getLanguageCode(newBase)
        LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            val savedLanguageCode = LocaleHelper.getLanguageCode(this)
            var languageCode by remember { mutableStateOf(savedLanguageCode) }

            var authStateVersion by remember { mutableStateOf(0) }
            var currentUserId by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

            DisposableEffect(Unit) {
                val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                    val newUserId = auth.currentUser?.uid
                    Log.d("MainActivity", "AuthStateListener: nuevo userId = $newUserId")
                    if (currentUserId != newUserId) {
                        currentUserId = newUserId
                        authStateVersion++
                    }
                }
                FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

                onDispose {
                    FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
                }
            }

            Log.d("MainActivity", "=== Recomposición ===")
            Log.d("MainActivity", "currentUserId: $currentUserId")

            var userHash by remember(currentUserId) { mutableStateOf<String?>(null) }
            var isLoading by remember(currentUserId) { mutableStateOf(true) }

            LaunchedEffect(currentUserId) {
                Log.d("MainActivity", "LaunchedEffect ejecutado con currentUserId: $currentUserId")
                isLoading = true
                if (currentUserId != null) {
                    Log.d("MainActivity", "Llamando a UserManager.createOrGetUser()")
                    UserManager.createOrGetUser(this@MainActivity) { hash ->
                        Log.d("MainActivity", "Hash recibido del UserManager: $hash")
                        userHash = hash
                        isLoading = false
                    }
                } else {
                    Log.d("MainActivity", "No hay usuario, seteando anon_user")
                    userHash = "anon_user"
                    isLoading = false
                }
            }

            Log.d("MainActivity", "isLoading: $isLoading, userHash: $userHash")

            TPFinalPabloVelazquezTheme(darkTheme = isDarkTheme) {
                if (isLoading) {
                    LoadingScreen()
                } else {
                    AppNavigation(
                        userHash = userHash ?: "anon_user",
                        isDarkTheme = isDarkTheme,
                        onThemeChange = { isDarkTheme = it },
                        language = LocaleHelper.getLanguageName(this, languageCode),
                        onLanguageChange = { newLanguageName ->
                            val newLanguageCode = when (newLanguageName) {
                                "Español" -> "es"
                                "English" -> "en"
                                else -> "es"
                            }

                            languageCode = newLanguageCode
                            LocaleHelper.setLocale(this@MainActivity, newLanguageCode)
                            recreate()
                        },
                        onLogout = {
                            Log.d("MainActivity", "onLogout callback ejecutado")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}