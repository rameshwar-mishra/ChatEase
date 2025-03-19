package com.example.chatease.trackers

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : Service() {

    private val binder = NotificationBinder()
    private val auth = FirebaseAuth.getInstance()
    private val rtDB = FirebaseDatabase.getInstance()
    private lateinit var databaseRef: DatabaseReference
    private var valueListener: ValueEventListener? = null
    private var appOpeningTimeStamp = System.currentTimeMillis()

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

            CoroutineScope(Dispatchers.IO).launch {
                valueListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        notificationManagement(currentUser = currentUser, snapshot = snapshot)
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
    }

    private fun notificationManagement(currentUser: FirebaseUser, snapshot: DataSnapshot) {
        if (snapshot.exists()) {
            for (doc in snapshot.children) {
                val messageTimeStamp = doc.child("lastMessageTimestamp").getValue(Long::class.java) ?: 0L
                if (messageTimeStamp < appOpeningTimeStamp) {
                    return
                }
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

                    if (haveIReadThisMessage) return //I have already seen this message

                    // Unread / Unseen messages
                    getSenderDetails(lastMessageSenderID) { userDetails_For_Notification ->
                        if (userDetails_For_Notification != null) {
                            avatar = userDetails_For_Notification.avatar
                            displayName = userDetails_For_Notification.displayName
                        }

                        val currentChatPartner =
                            getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE)
                                .getString("chatPartnerID", null)

                        val notification = Notification()
                        if (lastMessageSenderID != currentChatPartner && lastMessageSenderID != currentUser.uid) {
                            if (lastMessage.length > 30) {
                                notification.showNotification(
                                    title = displayName,
                                    body = lastMessage.substring(0, 30) + "...",
                                    icon = avatar,
                                    senderID = lastMessageSenderID,
                                    messageID = lastMessageID,
                                    context = this@NotificationService,
                                    notificationMode = "Online"
                                )
                            } else {
                                notification.showNotification(
                                    title = displayName,
                                    body = lastMessage,
                                    icon = avatar,
                                    senderID = lastMessageSenderID,
                                    messageID = lastMessageID,
                                    context = this@NotificationService,
                                    notificationMode = "Online"
                                )
                            }
                        }
                    }

                }
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

    inner class NotificationBinder : Binder() {
        fun getService(): NotificationService {
            return this@NotificationService
        }
    }
}