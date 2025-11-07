package com.example.tpfinal_pablovelazquez.data
import com.google.firebase.firestore.Exclude

data class SubscribedAlert(
    @get:Exclude var id: String = "",
    val userHash: String? = null,
    val date: String? = null,
    val location: String? = null,
    val photoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null,
    val comments: Map<String, String>? = null
) {
    constructor() : this("", null, null, null, null, null, null, null)
}
