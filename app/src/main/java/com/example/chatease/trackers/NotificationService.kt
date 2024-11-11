package com.example.chatease.trackers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationService : Service() {

    private val binder = NotificationBinder()
    private val auth = FirebaseAuth.getInstance()
    private val rtDB = FirebaseDatabase.getInstance()
    private lateinit var databaseRef: DatabaseReference
    private var valueListener: ValueEventListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        unReadMessageListener()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        valueListener?.let {
            databaseRef.removeEventListener(it)
        }
        valueListener = null
        super.onDestroy()
    }

    val messageIDSet = mutableSetOf<String>()

    private fun unReadMessageListener() {
        auth.currentUser?.let { currentUser ->
            databaseRef = rtDB.getReference("chats")
            valueListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (doc in snapshot.children) {
                            val lastMessageID = doc.child("lastMessageID").getValue(String::class.java) ?: ""
                            val haveIReadThisMessage =
                                doc.child("unRead_By_${currentUser.uid}").getValue(Boolean::class.java) ?: false

                            if (messageIDSet.contains(lastMessageID) && haveIReadThisMessage) {
                                messageIDSet.remove(lastMessageID)
                            }

                            if (!messageIDSet.contains(lastMessageID)) {
                                messageIDSet.add(lastMessageID)
                                val lastMessage = doc.child("lastMessage").getValue(String::class.java) ?: ""
                                val lastMessageSenderID = doc.child("lastMessageSender").getValue(String::class.java) ?: ""

                                var avatar = ""
                                var displayName = ""
                                if (!haveIReadThisMessage) {
                                    getSenderDetails(lastMessageSenderID) { userDetails_For_Notification ->
                                        if (userDetails_For_Notification != null) {
                                            avatar = userDetails_For_Notification.avatar
                                            displayName = userDetails_For_Notification.displayName
                                        }

                                        val currentChatPartner =
                                            getSharedPreferences("chatTracker", MODE_PRIVATE).getString(
                                                "chatPartnerID",
                                                null
                                            )

                                        if (lastMessageSenderID != currentChatPartner && lastMessageSenderID != currentUser.uid) {
                                            if (lastMessage.length > 30) {
                                                showNotification(
                                                    title = displayName,
                                                    body = lastMessage.substring(0, 30) + "...",
                                                    icon = avatar,
                                                    senderID = lastMessageSenderID,
                                                    messageID = lastMessageID
                                                )
                                            } else {
                                                showNotification(
                                                    title = displayName,
                                                    body = lastMessage,
                                                    icon = avatar,
                                                    senderID = lastMessageSenderID,
                                                    messageID = lastMessageID
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            valueListener?.let {
                databaseRef.orderByChild("participants/${currentUser.uid}")
                    .equalTo(true)
                    .addValueEventListener(valueListener as ValueEventListener)

            }
        }
    }

    data class UserDetails(val avatar: String, val displayName: String)

    private fun getSenderDetails(userID: String, callback: (UserDetails?) -> (Unit)) {
        rtDB.getReference("users/$userID").get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val avatar = task.result.child("avatar").getValue(String::class.java) ?: ""
                    val displayName = task.result.child("displayName").getValue(String::class.java) ?: ""
                    callback(UserDetails(avatar, displayName))
                } else {
                    callback(null)
                }
            }
    }

    private fun showNotification(title: String, body: String, icon: String, senderID: String, messageID: String) {
        val channel = NotificationChannel("1", "Message Notifications", NotificationManager.IMPORTANCE_HIGH)
        val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        Thread {
            try {
                lateinit var largeIconBitmap: Bitmap
                if (icon == "" || icon == null) {
                    largeIconBitmap = Glide.with(this)
                        .asBitmap()
                        .load(R.drawable.vector_default_user_avatar)
                        .submit()
                        .get()
                } else {
                    largeIconBitmap = Glide.with(this)
                        .asBitmap()
                        .load(icon)
                        .submit()
                        .get()
                }

                Handler(Looper.getMainLooper()).post({
                    // Post the notification
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        val notificationIntent = Intent(this, ChatActivity::class.java).apply {
                            putExtra("id", senderID)
                            putExtra("fromNotification", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        val notificationPendingIntent = PendingIntent.getActivity(
                            this,
                            senderID.hashCode(),
                            notificationIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val clearIntent = Intent(this@NotificationService, NotificationClearReceiver::class.java).apply {
                            action = "clear_notification"
                            putExtra("senderID", senderID)
                        }
                        val clearPendingIntent = PendingIntent.getBroadcast(
                            this@NotificationService,
                            0,
                            clearIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )


                        // Create the individual notification builder
                        val builder = NotificationCompat.Builder(this, "1").apply {
                            setContentTitle(title)
                            setContentText(body)
                            setSmallIcon(R.drawable.chatease_logo)
                            setLargeIcon(largeIconBitmap)
                            setAutoCancel(true)
                            setContentIntent(notificationPendingIntent)
                            setDeleteIntent(clearPendingIntent)
                            setGroup("com.example.chatease.MESSAGE_GROUP") // Group key for notifications
                        }

                        // Add the notification to active notifications set
                        ActiveNotificationManager.activeNotification.add(messageID)

                        manager.notify(senderID.hashCode(), builder.build()) // Use unique ID for each notification

                        // Create group summary notification if there are multiple notifications
                        if (ActiveNotificationManager.activeNotification.size > 1) {
                            val groupNotification = NotificationCompat.Builder(this, "1").apply {
                                setContentTitle("You have new messages")
                                setContentText("From: $senderID")
                                setSmallIcon(R.drawable.chatease_logo)
                                setStyle(NotificationCompat.InboxStyle().setSummaryText("New Messages"))
                                setGroup("com.example.chatease.MESSAGE_GROUP") // Same group key as individual notifications
                                setGroupSummary(true) // Mark this as the group summary
                                setAutoCancel(true)
                            }

                            // Use the same ID for the group summary notification
                            manager.notify(0, groupNotification.build()) // Group summary ID should be constant
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

    }

    inner class NotificationBinder : Binder() {
        fun getService(): NotificationService {
            return this@NotificationService
        }
    }
}

class NotificationClearReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val senderID = intent?.getStringExtra("senderID")
        if (senderID != null) {
            ActiveNotificationManager.activeNotification.remove(senderID)
        }
    }
}