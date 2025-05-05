package com.example.chatease.trackers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.ChatActivity
import com.example.chatease.activities.MainActivity

class Notification() {
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        icon: String,
        senderID: String,
        messageID: String,
        notificationMode: String
    ) {
        val channel = NotificationChannel("1", "Message Notifications", NotificationManager.IMPORTANCE_HIGH)
        val manager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        Thread {
            try {
                lateinit var largeIconBitmap: Bitmap
                if (icon == "" || icon == null) {
                    largeIconBitmap = Glide.with(context)
                        .asBitmap()
                        .load(R.drawable.vector_default_user_avatar)
                        .submit()
                        .get()
                } else {
                    largeIconBitmap = Glide.with(context)
                        .asBitmap()
                        .load(icon)
                        .submit()
                        .get()
                }

                Handler(Looper.getMainLooper()).post({
                    // Post the notification
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        val notificationIntent = Intent(context, ChatActivity::class.java).apply {
                            putExtra("id", senderID)
                            putExtra("fromNotification", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        val notificationPendingIntent = PendingIntent.getActivity(
                            context,
                            senderID.hashCode(),
                            notificationIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val clearIntent = Intent(context, NotificationClearReceiver::class.java).apply {
                            action = "clear_notification"
                            putExtra("senderID", senderID)
                        }

                        val clearPendingIntent = PendingIntent.getBroadcast(
                            context,
                            0,
                            clearIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )

                        val groupIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        val groupPendingIntent = PendingIntent.getActivity(
                            context,
                            senderID.hashCode(),
                            groupIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )


                        // Create the individual notification builder
                        val builder = NotificationCompat.Builder(context, "1").apply {
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
                        TrackerSingletonObject.activeNotification.add(messageID)

                        manager.notify(senderID.hashCode(), builder.build()) // Use unique ID for each notification

                        // Create group summary notification if there are multiple notifications
                        if (TrackerSingletonObject.activeNotification.size > 1) {
                            val groupNotification = NotificationCompat.Builder(context, "1").apply {
                                setContentTitle("You have new messages")
                                setContentText("From: $senderID")
                                setSmallIcon(R.drawable.chatease_logo)
                                setStyle(NotificationCompat.InboxStyle().setSummaryText("New Messages"))
                                setGroup("com.example.chatease.MESSAGE_GROUP") // Same group key as individual notifications
                                setContentIntent(groupPendingIntent)
                                setGroupSummary(true) // Mark this as the group summary
                                setAutoCancel(true)
                            }

                            // Use the same ID for the group summary notification
                            manager.notify(0, groupNotification.build()) // Group summary ID should be constant
                        }

                        if(TrackerSingletonObject.hasMessageArrived.get()) {
                            TrackerSingletonObject.hasMessageArrived.set(false)
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

    }
}

class NotificationClearReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val senderID = intent?.getStringExtra("senderID")
        if (senderID != null) {
            TrackerSingletonObject.activeNotification.remove(senderID)
        }
    }
}