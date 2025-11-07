package com.example.tpfinal_pablovelazquez.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.example.tpfinal_pablovelazquez.R

@Composable
fun LoginScreen(navController: NavController) {
    val activity = LocalActivity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8E8E8)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginCard(activity = activity, navController = navController)
        }
    }
}

@Composable
fun LoginCard(activity: Activity?, navController: NavController) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    fun handleLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = context.getString(R.string.invalid_hash)
            return
        }
        if (activity == null) {
            errorMessage = "Error de configuración de la app."
            return
        }
        isLoading = true
        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(activity) { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_LONG).show()
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    errorMessage = task.exception?.message ?: "Error de autenticación."
                }
            }
    }

    fun handleRegistration() {
        if (name.isBlank() || lastName.isBlank() || username.isBlank() || password.isBlank()) {
            errorMessage = context.getString(R.string.invalid_hash)
            return
        }
        if (activity == null) {
            errorMessage = "Error de configuración de la app."
            return
        }
        isLoading = true
        auth.createUserWithEmailAndPassword(username, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener

                    val userData = hashMapOf(
                        "uid" to userId,
                        "name" to name,
                        "lastName" to lastName,
                        "email" to username,
                        "hashId" to UUID.randomUUID().toString()
                    )

                    firestore.collection("users")
                        .document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            isLoading = false
                            Toast.makeText(context, context.getString(R.string.subscribed_success), Toast.LENGTH_LONG).show()
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            errorMessage = "Error al guardar datos: ${e.message}"
                        }

                } else {
                    isLoading = false
                    errorMessage = task.exception?.message ?: "Error en el registro."
                }
            }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLoginMode) stringResource(R.string.login) else stringResource(R.string.create_account),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            AnimatedVisibility(!isLoginMode) {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.first_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text(stringResource(R.string.last_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    )
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.email)) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.email)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            )

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (isLoginMode) handleLogin() else handleRegistration()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) stringResource(R.string.enter) else stringResource(R.string.create_account),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                },
                modifier = Modifier.padding(top = 8.dp),
                enabled = !isLoading
            ) {
                Text(
                    text = if (isLoginMode)
                        stringResource(R.string.no_account_yet)
                    else
                        stringResource(R.string.already_have_account)
                )
            }
        }
    }
}
