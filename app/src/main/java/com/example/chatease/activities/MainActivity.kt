package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.R
import com.example.chatease.databinding.ActivityMainBinding
import com.example.chatease.dataclass.RecentChatData
import com.example.chatease.recyclerview_adapters.RecentChatAdapter
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    // View binding for accessing layout elements
    private lateinit var binding: ActivityMainBinding

    // Firebase authentication instance to manage user authentication
    private val auth = FirebaseAuth.getInstance()

    // Firestore database instance to interact with Firestore database
    private val db = FirebaseFirestore.getInstance()

    private val rtDB = FirebaseDatabase.getInstance()

    // RecyclerView for displaying recent chats
    private lateinit var recyclerView: RecyclerView

    // Variable to hold modified chat data temporarily
    private var modifiedChatData: RecentChatData? = null

    // Mutable list to hold recent chat data
    private var recentChatDataList = mutableListOf<RecentChatData>()

    private lateinit var chatUserIDs: MutableSet<String>

    private lateinit var adapter: RecentChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout and set it as the content view using view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up window insets to ensure layout is not obscured by system bars (like status bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the toolbar for the activity
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        // Initialize the RecyclerView and set its layout manager to display items vertically
        recyclerView = findViewById(R.id.recyclerViewRecentChat)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Initialize the adapter for the RecyclerView with the recent chat data list
        adapter = RecentChatAdapter(this, recentChatDataList)
        recyclerView.adapter = adapter

        chatUserIDs = mutableSetOf()

        // Set up a Firestore listener to fetch chats for the current user
        listenForChatUpdates()

        // Set up the search button click listener to start the SearchActivity
        binding.floatingActionButtonSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun listenForChatUpdates() {

        rtDB.getReference("chats").orderByChild("participants/${auth.currentUser!!.uid}")
            .equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("CHAT_UPDATES", "WORKS")
                    if (snapshot.exists()) {
                        for (convo in snapshot.children) {
                            updateRecentChatData(convo)
                            Log.d("CHAT_UPDATES_LOOP", "WORKS")
                            updateChatIDs(convo)
                        }

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Error fetching chats: ${error.message}")
                }

            })
    }

    private fun updateChatIDs(document: DataSnapshot) {
        val participantsSnapshot = document.child("participants")
        if (participantsSnapshot.exists()) {
            for (participant in participantsSnapshot.children) {
                chatUserIDs.add(participant.key!!)
            }
            listenForUserProfileUpdates()
        } else {
            Log.d("MainActivity", "Participants list is null or empty")
        }
    }

    private fun listenForUserProfileUpdates() {
        for (userID in chatUserIDs) {
            Log.d("USER_PROFILE_UPDATES", "WORKS")
            rtDB.getReference("chats").child(userID)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("USER_PROFILE_LISTENER_UPDATES", "WORKS")
                        if (snapshot.exists()) {
                            updateRecentChatsForUser(
                                userID = userID,
                                userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: "",
                                userDisplayName = snapshot.child("displayName").getValue(String::class.java) ?: ""
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MainActivity", "Error fetching user profile: ${error.message}")
                    }

                })
        }
    }

    data class UserProfile(
        val avatar: String,
        val displayName: String
    )

    private val userProfileCache = mutableMapOf<String, UserProfile>()

    private fun updateRecentChatsForUser(userID: String, userAvatar: String, userDisplayName: String) {
        // Update the local cache
        userProfileCache[userID] = UserProfile(avatar = userAvatar, displayName = userDisplayName)
    }

    // Update the recent chat data based on the changes in Firestore documents
    private fun updateRecentChatData(snapshot: DataSnapshot) {
        // Extract chat details from the document
        val lastMessage = snapshot.child("lastMessage").getValue(String::class.java) ?: ""
        val lastMessageSender = snapshot.child("lastMessageSender").getValue(String::class.java) ?: ""
        val lastMessageTimestamp =
            (snapshot.child("lastMessageTimestamp").getValue(Long::class.java) ?: 0L) / 1000   // MiliSeconds to Seconds

        val formattedTimestamp = getRelativeTime(Timestamp(lastMessageTimestamp, 0)) // Format the timestamp for display

        // Get participants of the chat and identify the current user and the other participant
        val participantsSnapshot = snapshot.child("participants")
        val participants = participantsSnapshot.children.map { it.key }
        val otherParticipant = participants.first { it != auth.currentUser!!.uid }
        val thisParticipant = participants.first { it == auth.currentUser!!.uid }

        Log.d("LAST_MESSAGE", lastMessage)
        Log.d("LAST_MESSAGE_SENDER", lastMessage)
        Log.d("LAST_MESSAGE_TIMESTAMP", formattedTimestamp)

        Log.d("updateRecentChatData", "WORKS")

        // Nested Database Fetch (Need to optimize later)

        rtDB.getReference("users").child(otherParticipant!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (lastMessageSender == otherParticipant) {
                        Log.d("updateRecentChatData_OTHER_PARTICIPANT", "WORKS")
                        updateRecentChatDataList(
                            userID = otherParticipant,
                            displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                            avatarUrl = snapshot.child("avatar").getValue(String::class.java) ?: "",
                            lastMessage = lastMessage,
                            senderDisplayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                            formattedTimestamp = formattedTimestamp,
                            lastMessageTimestamp = lastMessageTimestamp
                        )
                    } else {
                        Log.d("updateRecentChatData_THIS_PARTICIPANT", "WORKS")
                        updateRecentChatDataList(
                            userID = otherParticipant!!,
                            displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                            avatarUrl = snapshot.child("avatar").getValue(String::class.java) ?: "",
                            lastMessage = lastMessage,
                            senderDisplayName = "You",
                            formattedTimestamp = formattedTimestamp,
                            lastMessageTimestamp = lastMessageTimestamp
                        )
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        // Fetch user details of both participants based on the last message sender
        // Use the cached user data in userProfileCache


    }


    private fun updateRecentChatDataList(
        userID: String,
        displayName: String,
        avatarUrl: String,
        lastMessage: String,
        senderDisplayName: String,
        formattedTimestamp: String,
        lastMessageTimestamp: Long
    ) {
        // Check if chat already exists, update if necessary, or add a new entry
        val existingChatIndex = recentChatDataList.indexOfFirst { it.id == userID }

        if (existingChatIndex != -1) {
            // Update existing chat
            recentChatDataList[existingChatIndex] = RecentChatData(
                id = userID,
                displayName = displayName,
                avatar = avatarUrl,
                lastMessage = lastMessage,
                lastMessageSender = senderDisplayName,
                lastMessageTimeStamp = formattedTimestamp,
                timestamp = lastMessageTimestamp.toString()
            )
        } else {
            // Add new chat entry
            recentChatDataList.add(
                RecentChatData(
                    id = userID,
                    displayName = displayName,
                    avatar = avatarUrl,
                    lastMessage = lastMessage,
                    lastMessageSender = senderDisplayName,
                    lastMessageTimeStamp = formattedTimestamp,
                    timestamp = lastMessageTimestamp.toString()
                )
            )
        }

        // Sort chats by timestamp if needed
        recentChatDataList.sortByDescending { it.timestamp }

        // Notify the adapter about the data change
        adapter.notifyDataSetChanged()
    }

    // Inflate the options menu for the activity
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_signout, menu) // Inflate the sign-out menu
        return true
    }

    // Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settingsIcon) {
            // Start SettingsActivity when settings icon is clicked
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item) // Call super method for default behavior
    }

    // Get a relative time string based on the timestamp for display
    private fun getRelativeTime(timestamp: Timestamp): String {
        // Create calendar instance from the timestamp
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp.seconds * 1000 }
        val today = Calendar.getInstance() // Get current date

        // Formatters for time display
        val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return when {
            calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
                // Return formatted date if it's not the current year
                dateFormatter.format(calendar.time)
            }

            calendar.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) -> {
                // Return formatted date if it's not today
                dateFormatter.format(calendar.time)
            }

            else -> {
                // Return formatted time if it's today
                timeFormatter.format(calendar.time)
            }
        }
    }
}