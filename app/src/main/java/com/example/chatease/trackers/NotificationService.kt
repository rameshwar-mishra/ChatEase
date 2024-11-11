package com.example.chatease.trackers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
    lateinit var databaseRef: DatabaseReference
    private var valueListener: ValueEventListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        unReadMesssageListener()
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


    private fun unReadMesssageListener() {
        auth.currentUser?.let { currentUser ->
            databaseRef = rtDB.getReference("chats")
            valueListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (doc in snapshot.children) {
                            val lastMessage = doc.child("lastMessage").getValue(String::class.java) ?: ""
                            val lastMessageSenderID = doc.child("lastMessageSender").getValue(String::class.java) ?: ""
                            val haveIReadThisMessage =
                                doc.child("unRead_By_${currentUser.uid}").getValue(Boolean::class.java) ?: false
                            var avatar = ""
                            var displayName = ""
                            if (!haveIReadThisMessage) {
                                getSenderDetails(lastMessageSenderID) { userDetails_For_Notification ->
                                    if (userDetails_For_Notification != null) {
                                        avatar = userDetails_For_Notification.avatar
                                        displayName = userDetails_For_Notification.displayName
                                    }

                                    val currentChatPartner =
                                        getSharedPreferences("chatTracker", MODE_PRIVATE).getString("chatPartnerID", null)
                                    // KAAM KARNA HAI

                                    if (lastMessageSenderID != currentChatPartner && lastMessageSenderID != currentUser.uid) {
                                        if (lastMessage.length > 30) {
                                            showNotification(
                                                title = displayName,
                                                body = lastMessage.substring(0, 30) + "...",
                                                icon = avatar,
                                                senderID = lastMessageSenderID
                                            )
                                        } else {
                                            showNotification(
                                                title = displayName,
                                                body = lastMessage,
                                                icon = avatar,
                                                senderID = lastMessageSenderID
                                            )
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

    private fun showNotification(title: String, body: String, icon: String, senderID: String) {
        val channel = NotificationChannel("1", "Notification", NotificationManager.IMPORTANCE_HIGH)
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
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("id", senderID)
                        putExtra("fromNotification",true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val builder = NotificationCompat.Builder(this, "1").apply {
                        setContentTitle(title)
                        setContentText(body)
                        setSmallIcon(R.drawable.vector_icon_emoji)
                        setLargeIcon(largeIconBitmap)
                        setAutoCancel(true)
                        setContentIntent(pendingIntent)
                        build()
                    }
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        manager.notify(1, builder.build())
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