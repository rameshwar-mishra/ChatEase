package com.example.chatease.trackers

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.chatease.activities.ChatActivity
import com.example.chatease.activities.SignInActivity
import com.example.chatease.activities.SignUpActivity
import com.example.chatease.adapters_recyclerview.RecentGroupChatAdapter
import com.example.chatease.dataclass.RecentGroupChatData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class AppStatusTracker : Application(), Application.ActivityLifecycleCallbacks {

    private lateinit var rtDB: FirebaseDatabase
    private var currentUser: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth
    private var activityCount = AtomicInteger(0)
    private var lastStatus: String? = null
    private var isActivityChangingConfiguration = false
    private var notificationService: NotificationService? = null
    private var serviceConnection: ServiceConnection? = null
    private var serviceBound = false

    private lateinit var adapter: RecentGroupChatAdapter
    private val groupList = mutableListOf<RecentGroupChatData>()
    val groupListLiveData = MutableLiveData<MutableList<RecentGroupChatData>>()
    private val groupIDSet = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        rtDB = FirebaseDatabase.getInstance()
        rtDB.setPersistenceEnabled(true)
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as NotificationService.NotificationBinder
                notificationService = binder.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceBound = false
                notificationService = null
            }
        }
        registerAuthListener()
        setupConnectionListener()
        registerActivityLifecycleCallbacks(this)
    }

    private fun registerAuthListener() {
        auth.addAuthStateListener { authStatus ->
            currentUser = authStatus.currentUser
            if (currentUser == null)
                updateStatus("Offline")
        }
    }

    private fun setupConnectionListener() {
        currentUser?.let { user ->
            rtDB.getReference(".info/connected")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val connected = snapshot.getValue(Boolean::class.java) ?: false
                        if (connected && currentUser != null) {
                            setupOnlineStatusWithOnDisconnect()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    fun setupOnlineStatusWithOnDisconnect() {
        currentUser?.let { user ->
            val userRef = rtDB.getReference("users").child(user.uid)
            userRef.child("status").onDisconnect().setValue("Offline")
            userRef.child("lastHeartBeat").onDisconnect().setValue(ServerValue.TIMESTAMP)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        currentUser?.let {
            if (!isActivityChangingConfiguration && activity !is SignInActivity && activity !is SignUpActivity) {
                if (activityCount.incrementAndGet() == 1) {
                    // This service will execute when the app is in the foreground
                    if (TrackerSingletonObject.isChatActivityOpenedViaNotification.get()) {
                        // if user opened the chat activity via notification
                        statusOnline(activity)
                    } else {
                        // if a notification arrived from FireBase Cloud Messaging, which wake up the app.
                        // Delayed the execution after waking the app to verify if the user opened the app or it's a notification wake up
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!TrackerSingletonObject.hasMessageArrived.get()) {
                                statusOnline(activity)
                            }
                        }, 2000L)
                    }
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        currentUser?.let {
            isActivityChangingConfiguration = activity.isChangingConfigurations
            if (!isActivityChangingConfiguration && activity !is SignInActivity && activity !is SignUpActivity) {
                if (activityCount.decrementAndGet() == 0) {
                    // This service will execute when the app is in the background
                    stopNotificationService(activity)
                    updateStatus("Offline")
                    TrackerSingletonObject.isAppForeground.set(false)

                    if (TrackerSingletonObject.isTyping.get()) {
                        ChatActivity().setTypingStatus(false)
                        TrackerSingletonObject.isTyping.set(false)
                    }
                }
            }
        }
    }

    private fun statusOnline(activity: Activity) {
        startNotificationService(activity)
        updateStatus("Online")
        TrackerSingletonObject.isAppForeground.set(true)
        auth.currentUser?.let { currentUser ->
            groupListener(currentUserID = currentUser.uid)
        }
    }

    private fun updateStatus(status: String) {
        currentUser?.let { user ->
            if (status != lastStatus) {
                lastStatus = status
                rtDB.getReference("users").child(user.uid)
                    .updateChildren(
                        mapOf(
                            "status" to status,
                            "lastHeartBeat" to ServerValue.TIMESTAMP
                        )
                    )
            }
        }
    }

    private fun startNotificationService(activity: Activity) {
        if (!serviceBound) {
            val intent = Intent(activity, NotificationService::class.java)
            serviceConnection?.let {
                startService(intent)
                activity.bindService(intent, it, Context.BIND_AUTO_CREATE)
                serviceBound = true
            }
        }
    }

    private fun stopNotificationService(activity: Activity) {
        if (serviceBound) {
            val intent = Intent(activity, NotificationService::class.java)
            serviceConnection?.let {
                stopService(intent)
                try {
                    activity.unbindService(it)
                } catch (e: Exception) {
                }

                serviceBound = false
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onTerminate() {
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(this)
        auth.removeAuthStateListener { registerAuthListener() }
    }

    private fun groupListener(currentUserID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            rtDB.getReference("groups").orderByChild("metadata/participants/${currentUserID}").equalTo(true)
                .addChildEventListener(object : ChildEventListener {

                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        if (snapshot.exists()) {
                            updateRecentGroupChatList(snapshot = snapshot, currentUserID = currentUserID)
                            groupNotificationSubscription(groupID = snapshot.key!!, status = true)
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        updateRecentGroupChatList(snapshot = snapshot, currentUserID = currentUserID)
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        for (group in groupList) {
                            if (group.id == snapshot.key) {
                                groupNotificationSubscription(groupID = group.id, status = false)
                                groupList.remove(group)
                                groupListLiveData.postValue(groupList)
                                break
                            }
                        }
                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                    override fun onCancelled(error: DatabaseError) {}

                })
        }
    }

    private var participantsName = mutableMapOf<String, String>()

    private fun updateRecentGroupChatList(snapshot: DataSnapshot, currentUserID: String) {

        val lastMessageSender = snapshot.child("metadata/lastMessageSender").getValue(String::class.java) ?: ""

        if (participantsName[lastMessageSender] == null) {
            rtDB.getReference("users/$lastMessageSender/displayName").get()
                .addOnSuccessListener { snapshot1 ->
                    participantsName[lastMessageSender] = snapshot1.value as? String? ?: ""
                    getDisplayNameAndUpdateGroupList(
                        snapshot = snapshot,
                        currentUserID = currentUserID,
                        displayName = participantsName[lastMessageSender]
                    )
                }
        } else {
            getDisplayNameAndUpdateGroupList(
                snapshot = snapshot,
                currentUserID = currentUserID,
                displayName = participantsName[lastMessageSender]
            )
        }
    }

    private fun getDisplayNameAndUpdateGroupList(snapshot: DataSnapshot, currentUserID: String, displayName: String?) {

        val lastMessageTimestamp = (snapshot.child("metadata/lastMessageTimestamp").getValue(Long::class.java)
            ?: 0L) / 1000   // MilliSeconds to Seconds

        val createdAt = (snapshot.child("metadata/createdAt").getValue(Long::class.java)
            ?: 0L) / 1000   // MilliSeconds to Seconds

        var formattedTimestamp = ""
        if (lastMessageTimestamp != 0L) {
            formattedTimestamp = getRelativeTime(Timestamp(lastMessageTimestamp, 0)) // Format the timestamp for display
        } else {
            formattedTimestamp = getRelativeTime(Timestamp(createdAt, 0)) // Format the timestamp for display
        }

        // Check if chat already exists, update if exists, or add a new entry
        val existingChatIndex = groupList.indexOfFirst { it.id == snapshot.key }

        var timestamp = (snapshot.child("metadata/lastMessageTimestamp").getValue(Long::class.java) ?: 0L) / 1000
        if (timestamp == 0L) {
            timestamp = createdAt
        }

        val recentChatData = RecentGroupChatData(
            id = snapshot.key!!,
            groupName = snapshot.child("metadata/groupName").getValue(String::class.java) ?: "",
            groupIcon = snapshot.child("metadata/groupIcon").getValue(String::class.java) ?: "",
            lastMessage = snapshot.child("metadata/lastMessage").getValue(String::class.java) ?: "",
            lastMessageSender = displayName ?: "",
            isLastMessageReadByMe = snapshot.child("metadata/readReceipt/${currentUserID}").getValue(Boolean::class.java) ?: false,
            lastMessageTimeStamp = formattedTimestamp,
            timestamp = timestamp.toString(),
        )
        // update if chat already exists, or add a new entry
        if (existingChatIndex != -1) {

            groupList[existingChatIndex] = recentChatData
        } else {
            groupList.add(recentChatData)
        }

        // Sort chats by timestamp
        groupList.sortByDescending { it.timestamp }


        groupListLiveData.postValue(groupList)
    }

    fun getGroupLiveData(): LiveData<MutableList<RecentGroupChatData>> = groupListLiveData

    // Get a relative time string based on the timestamp for display
    private fun getRelativeTime(timestamp: Timestamp): String {
        // Create calendar instance from the timestamp
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp.seconds * 1000 }
        val today = Calendar.getInstance() // Get current date

        // Formatters for time display
        val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dateFormatterYear = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return when {
            calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
                // Not in the current year
                dateFormatterYear.format(calendar.time)
            }

            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                // Today
                timeFormatter.format(calendar.time)
            }

            calendar.get(Calendar.DAY_OF_YEAR) == (today.get(Calendar.DAY_OF_YEAR) - 1) -> {
                // Yesterday
                "Yesterday"
            }

            else -> {
                // Earlier this year
                dateFormatter.format(calendar.time)
            }
        }
    }

    private fun groupNotificationSubscription(groupID: String?, status: Boolean) {
        if (groupID != null) {

            val subscribedGroups = getSharedPreferences("groupSubscription", MODE_PRIVATE).getStringSet("groups", mutableSetOf()) ?: mutableSetOf()

            if (status) {
                if (!subscribedGroups.contains(groupID)) {
                    FirebaseMessaging.getInstance().subscribeToTopic(groupID)
                    subscribedGroups.add(groupID)
                }
            } else {
                if (subscribedGroups.contains(groupID)) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(groupID)
                    subscribedGroups.remove(groupID)
                }
            }

            getSharedPreferences("groupSubscription", MODE_PRIVATE).edit().putStringSet("groups", subscribedGroups).apply()
        }
    }
}