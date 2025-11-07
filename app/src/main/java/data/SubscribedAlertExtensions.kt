package com.example.tpfinal_pablovelazquez.data

fun SubscribedAlert.toAlert(): Alert {
    return Alert(
        id = 0,
        timestamp = this.timestamp ?: 0L,
        date = this.date ?: "",
        latitude = this.latitude ?: 0.0,
        longitude = this.longitude ?: 0.0,
        location = this.location ?: "",
        photoPath = this.photoPath ?: ""
    )
}
