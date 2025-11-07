package com.example.tpfinal_pablovelazquez.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tpfinal_pablovelazquez.R
import com.example.tpfinal_pablovelazquez.data.UserManager

@Composable
fun SettingsScreen(
    navController: NavController,
    userHash: String,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var otherHash by remember { mutableStateOf("") }
    var subscriptionStatus by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.your_hash), fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = userHash,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(userHash))
                        Toast.makeText(context, context.getString(R.string.hash_copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.hash_copied),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.subscribe_to_alerts), fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = otherHash,
            onValueChange = { otherHash = it },
            label = { Text(stringResource(R.string.paste_hash_here)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val invalidHashMsg = context.getString(R.string.invalid_hash)
                val cannotSubscribeOwnMsg = context.getString(R.string.cannot_subscribe_own)
                val successMsg = context.getString(R.string.subscribed_success)
                val errorMsg = context.getString(R.string.subscription_error)

                when {
                    otherHash.isBlank() -> Toast.makeText(context, invalidHashMsg, Toast.LENGTH_SHORT).show()
                    otherHash == userHash -> Toast.makeText(context, cannotSubscribeOwnMsg, Toast.LENGTH_SHORT).show()
                    else -> UserManager.followUser(userHash, otherHash) { success ->
                        subscriptionStatus = if (success) successMsg else errorMsg
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.subscribe)) }

        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = { navController.navigate("subscribed_management") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.view_subscriptions), modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.view_subscriptions))
        }

        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.dark_theme), fontSize = 16.sp)
            Switch(checked = isDarkTheme, onCheckedChange = { onThemeChange(it) })
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.language), fontSize = 16.sp)
            DropdownMenuBox(
                selectedLanguage = language,
                onLanguageSelected = onLanguageChange
            )
        }

        subscriptionStatus?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun DropdownMenuBox(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedLanguage)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Español") },
                onClick = {
                    onLanguageSelected("Español")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("English") },
                onClick = {
                    onLanguageSelected("English")
                    expanded = false
                }
            )
        }
    }
}