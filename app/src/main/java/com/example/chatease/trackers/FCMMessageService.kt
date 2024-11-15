package com.example.chatease.trackers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMMessageService : FirebaseMessagingService() {

    private val rtDB = FirebaseDatabase.getInstance()

    override fun onMessageReceived(message: RemoteMessage) {
        TrackerSingletonObject.hasMessageArrived.set(true)
        if (message.data.isNotEmpty()) {
            createNotification(
                senderID = message.data["senderID"] ?: "",
                lastMessage = message.data["lastMessage"] ?: "",
                messageID = message.data["messageID"] ?: ""
            )
        }
    }

    override fun onNewToken(token: String) {
        val currentFCMUserToken = getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE).getString(
            "FCMUserToken",
            null
        )

        if (currentFCMUserToken != token) {
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                rtDB.getReference("users").child(currentUser.uid).updateChildren(
                    mapOf(
                        "FCMUserToken" to token
                    )
                ).addOnSuccessListener {
                    getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE).edit().putString("FCMUserToken", token).apply()
                }
            }

        }
    }

    private fun createNotification(senderID: String, messageID: String, lastMessage: String) {
        rtDB.getReference("users").child(senderID)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    var lastMessageSubString = lastMessage

                    if (lastMessageSubString.length > 30) {
                        lastMessageSubString = lastMessageSubString.substring(0, 30)
                    }
                    Notification().showNotification(
                        context = this@FCMMessageService,
                        title = task.result.child("displayName").getValue(String::class.java) ?: "",
                        body = lastMessageSubString,
                        icon = task.result.child("avatar").getValue(String::class.java) ?: "",
                        senderID = senderID,
                        messageID = messageID,
                        notificationMode = "Offline"
                    )
                }
            }
    }
}