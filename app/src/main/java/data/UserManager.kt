package com.example.tpfinal_pablovelazquez.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.security.MessageDigest

object UserManager {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun generateUserHash(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(uid.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun createOrGetUser(context: Context, onResult: (String) -> Unit) {
        Log.d("UserManager", "=== createOrGetUser llamado ===")
        val currentUser = auth.currentUser
        Log.d("UserManager", "currentUser: ${currentUser?.uid}")

        if (currentUser != null) {
            val hash = generateUserHash(currentUser.uid)
            Log.d("UserManager", "Hash generado: $hash")

            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            saveUserHash(hash, prefs)
            registerFcmToken(hash)

            Log.d("UserManager", "Devolviendo hash: $hash")
            onResult(hash)
        } else {
            Log.d("UserManager", "No hay usuario, devolviendo anon_user")
            onResult("anon_user")
        }
    }

    private fun saveUserHash(hash: String, prefs: SharedPreferences) {
        Log.d("UserManager", "Guardando hash en SharedPreferences: $hash")

        val currentUser = auth.currentUser
        val displayName = currentUser?.displayName
            ?: currentUser?.email?.substringBefore("@")
            ?: "Usuario Anónimo"

        val userData = mapOf(
            "hash" to hash,
            "displayName" to displayName,
            "email" to currentUser?.email
        )

        firestore.collection("users").document(hash).set(userData, SetOptions.merge())
        prefs.edit().putString("user_hash", hash).apply()
    }

    private fun registerFcmToken(userHash: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("UserManager", "Token FCM registrado para hash: $userHash")
            firestore.collection("users")
                .document(userHash)
                .collection("fcm_tokens")
                .document(token)
                .set(mapOf("token" to token))
        }
    }

    fun getUserHash(context: Context): String {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("user_hash", null) ?: "anon_user"
    }

    fun followUser(currentUserHash: String, otherUserHash: String, onComplete: (Boolean) -> Unit) {
        val batch = firestore.batch()

        val followerRef = firestore.collection("users")
            .document(otherUserHash)
            .collection("followers")
            .document(currentUserHash)
        batch.set(followerRef, mapOf("hash" to currentUserHash))

        val subscriptionRef = firestore.collection("users")
            .document(currentUserHash)
            .collection("subscriptions")
            .document(otherUserHash)
        batch.set(subscriptionRef, mapOf("hash" to otherUserHash))

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }


    fun unfollowUser(currentUserHash: String, otherUserHash: String, onComplete: (Boolean) -> Unit) {
        val batch = firestore.batch()

        val followerRef = firestore.collection("users")
            .document(otherUserHash)
            .collection("followers")
            .document(currentUserHash)
        batch.delete(followerRef)

        val subscriptionRef = firestore.collection("users")
            .document(currentUserHash)
            .collection("subscriptions")
            .document(otherUserHash)
        batch.delete(subscriptionRef)

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getSubscriptions(currentUserHash: String, onResult: (List<String>) -> Unit) {
        firestore.collection("users")
            .document(currentUserHash)
            .collection("subscriptions")
            .get()
            .addOnSuccessListener { result ->
                val subscriptions = result.documents.mapNotNull { it.getString("hash") }
                onResult(subscriptions)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun resetUser(context: Context, onComplete: () -> Unit) {
        Log.d("UserManager", "=== resetUser llamado ===")
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        auth.signOut()
        Log.d("UserManager", "Usuario reseteado y logout exitoso")
        onComplete()
    }
}