package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
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

    // RecyclerView for displaying recent chats
    private lateinit var recyclerView: RecyclerView

    // Variable to hold modified chat data temporarily
    private var modifiedChatData: RecentChatData? = null
    // Mutable list to hold recent chat data
    private var recentChatDataList = mutableListOf<RecentChatData>()

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
        val adapter = RecentChatAdapter(this, recentChatDataList)
        recyclerView.adapter = adapter

        // Set up a Firestore listener to fetch chats for the current user
        db.collection("chats")
            .whereArrayContains("participants", auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, error ->
                // Handle any errors during the fetching of chats
                if (error != null) {
                    Log.d("MainActivity", "Error fetching chats: ${error.message}")
                    return@addSnapshotListener
                }

                // Process document changes in the fetched snapshot
                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        // Check the type of document change and update recent chat data accordingly
                        when (change.type) {
                            DocumentChange.Type.ADDED -> updateRecentChatData(change)  // New chat added
                            DocumentChange.Type.MODIFIED -> updateRecentChatData(change)  // Existing chat modified
                            DocumentChange.Type.REMOVED -> {
                                // Handle chat removal if needed (currently not implemented)
                            }
                        }
                    }
                }
            }

        // Set up the search button click listener to start the SearchActivity
        binding.floatingActionButtonSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    // Update the recent chat data based on the changes in Firestore documents
    private fun updateRecentChatData(change: DocumentChange) {
        // Extract chat details from the document
        val lastMessage = change.document.getString("lastMessage") ?: ""
        val lastMessageSender = change.document.getString("lastMessageSender") ?: ""
        val lastMessageTimestamp = change.document.getTimestamp("lastMessageTimestamp") ?: Timestamp.now()
        val formattedTimestamp = getRelativeTime(lastMessageTimestamp) // Format the timestamp for display

        // Get participants of the chat and identify the current user and the other participant
        val participants = change.document.get("participants") as List<String>
        // Determine which participant is the current user and which is the other participant
        val (thisParticipant, otherParticipant) = if (participants[0] == auth.currentUser?.uid) {
            participants[0] to participants[1]  // Current user is first participant
        } else {
            participants[1] to participants[0]  // Current user is second participant
        }

        // Fetch user details of both participants based on the last message sender
        var userDetailsTasks: List<Any>
        if (lastMessageSender == otherParticipant) {
            userDetailsTasks = listOf(
                db.collection("users").document(otherParticipant).get()  // Get only other participant details if they sent the last message
            )
        } else {
            // Get details for both participants if the current user sent the last message
            userDetailsTasks = listOf(
                db.collection("users").document(otherParticipant).get(),
                db.collection("users").document(thisParticipant).get()
            )
        }

        // Wait for all user detail fetch tasks to complete
        Tasks.whenAllComplete(userDetailsTasks).addOnCompleteListener { taskList ->
            if (taskList.isSuccessful) {
                // Extract user details from the fetched results
                val otherUserDetails = userDetailsTasks[0].result
                var currentUserDetails: DocumentSnapshot? = null
                var actualLastMessageSender: String

                // Determine the actual last message sender's username
                if (lastMessageSender == otherParticipant) {
                    actualLastMessageSender = otherUserDetails.getString("userName") ?: "" // Get username of other participant
                } else {
                    currentUserDetails = userDetailsTasks[1].result // Get current user details
                    actualLastMessageSender = currentUserDetails.getString("userName") ?: "" // Get username of current user
                }

                // Fetch display name and avatar of the other participant
                val displayName = otherUserDetails.getString("displayName") ?: ""
                val avatar = otherUserDetails.getString("avatar") ?: ""
                val userName = otherUserDetails.getString("userName") ?: ""

                // Create a RecentChatData object with the fetched data for the chat
                val recentChatData = RecentChatData(
                    id = otherParticipant,
                    userName = userName,
                    displayName = displayName,
                    avatar = avatar,
                    lastMessage = lastMessage,
                    lastMessageSender = actualLastMessageSender,
                    lastMessageTimeStamp = formattedTimestamp
                )

                // Update the recent chat data list based on the type of document change
                when (change.type) {
                    DocumentChange.Type.ADDED -> {
                        // Add new chat data to the beginning of the list
                        recentChatDataList.add(0, recentChatData)
                        Log.e("type", "ADDED: $recentChatDataList") // Log the updated list for debugging
                        Log.e("type", "ADDED SIZE: ${recentChatDataList.size}") // Log the size of the list
                    }

                    DocumentChange.Type.MODIFIED -> {
                        // Store modified chat data to update later
                        modifiedChatData = recentChatData
                    }

                    DocumentChange.Type.REMOVED -> {
                        // Currently not implemented for removed chats
                    }
                }

                // If there is modified chat data, update the list accordingly
                if (modifiedChatData != null) {
                    for (i in recentChatDataList.indices) {
                        // Remove the old chat data from the list
                        if (recentChatDataList[i].id == modifiedChatData!!.id) {
                            recentChatDataList.removeAt(i)
                            break
                        }
                    }
                    // Add the modified chat data to the beginning of the list
                    recentChatDataList.add(0, modifiedChatData!!)
                }
                // Notify the adapter that the data has changed to update the RecyclerView
                recyclerView.adapter?.notifyDataSetChanged()
            } else {
                // Log error if fetching user details failed
                Log.e("MainActivity", "Error fetching user details: ${taskList.exception?.message}")
            }
        }
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
