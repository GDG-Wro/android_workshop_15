package com.example.gdgandroidwebinar15

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseService : FirebaseMessagingService() {
    override fun onNewToken(s: String) {
        Log.i("FCM", "New token: $s")
        super.onNewToken(s)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i("FCM", "Message received: ${message.notification?.title ?: message.data}")
        super.onMessageReceived(message)
    }
}
