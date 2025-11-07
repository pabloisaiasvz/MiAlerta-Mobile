package com.example.tpfinal_pablovelazquez.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreHelper {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun uploadAlertToFirestore(alert: com.example.tpfinal_pablovelazquez.data.Alert, creatorHash: String) {
        val alertData = mapOf(
            "creatorHash" to creatorHash,
            "timestamp" to alert.timestamp,
            "date" to alert.date,
            "latitude" to alert.latitude,
            "longitude" to alert.longitude,
            "location" to alert.location,
            "photoPath" to alert.photoPath
        )
        firestore.collection("alerts").add(alertData).await()
    }

    suspend fun getFollowerTokens(userHash: String): List<String> {
        val followersSnapshot = firestore.collection("users")
            .document(userHash)
            .collection("followers")
            .get().await()

        val tokens = mutableListOf<String>()
        for (follower in followersSnapshot.documents) {
            val followerHash = follower.getString("hash") ?: continue
            val tokenSnapshot = firestore.collection("users")
                .document(followerHash)
                .collection("fcm_tokens")
                .get().await()
            tokenSnapshot.documents.forEach { doc ->
                doc.getString("token")?.let { tokens.add(it) }
            }
        }
        return tokens
    }

    suspend fun sendAlertNotification(userHash: String, message: String) {
        val tokens = getFollowerTokens(userHash)
        FCMService.sendPushNotification(tokens, message)
    }
}
