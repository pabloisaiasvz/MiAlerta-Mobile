package com.example.tpfinal_pablovelazquez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tpfinal_pablovelazquez.data.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import androidx.compose.ui.res.stringResource
import com.example.tpfinal_pablovelazquez.R

data class SubscriptionInfo(
    val hash: String,
    val displayName: String
)

private suspend fun getSubscriptionsSuspending(userHash: String): List<String> =
    suspendCancellableCoroutine { continuation ->
        UserManager.getSubscriptions(userHash) { hashes ->
            if (continuation.isActive) {
                continuation.resume(hashes)
            }
        }
    }

@Composable
fun SubscribedManagementScreen(navController: NavController, userHash: String) {
    val context = LocalContext.current
    var subscriptions by remember { mutableStateOf(listOf<SubscriptionInfo>()) }
    var loading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(userHash) {

        try {
            val hashes = getSubscriptionsSuspending(userHash)

            val subscriptionsWithNames = mutableListOf<SubscriptionInfo>()

            for (hash in hashes) {
                try {
                    val userDoc = firestore.collection("users")
                        .document(hash)
                        .get()
                        .await()

                    val displayName = userDoc.getString("displayName")
                        ?: userDoc.getString("email")?.substringBefore("@")
                        ?: "Usuario desconocido"

                    subscriptionsWithNames.add(SubscriptionInfo(hash, displayName))
                } catch (e: Exception) {
                    subscriptionsWithNames.add(SubscriptionInfo(hash, "Usuario desconocido"))
                }
            }
            subscriptions = subscriptionsWithNames

        } catch (e: Exception) {
            subscriptions = emptyList()
            Toast.makeText(context, "Error al cargar suscripciones", Toast.LENGTH_SHORT).show()
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.view_subscriptions),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                color = MaterialTheme.colorScheme.primary,
                thickness = 2.dp
            )
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            subscriptions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No estás suscripto a ningún usuario.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subscriptions) { subscription ->
                        SubscriptionItem(
                            subscriptionInfo = subscription,
                            onUnsubscribe = {
                                UserManager.unfollowUser(userHash, subscription.hash) { success ->
                                    if (success) {
                                        subscriptions = subscriptions.filter { it.hash != subscription.hash }
                                        Toast.makeText(context, "Se canceló la suscripción", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error al cancelar suscripción", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionItem(subscriptionInfo: SubscriptionInfo, onUnsubscribe: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Usuario",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscriptionInfo.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subscriptionInfo.hash,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = onUnsubscribe) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Cancelar Suscripción",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}