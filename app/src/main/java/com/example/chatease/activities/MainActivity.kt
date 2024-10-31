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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    // View binding for accessing layout elements
    private lateinit var binding: ActivityMainBinding

    // Firebase authentication instance
    private val auth = FirebaseAuth.getInstance()

    // Firestore database instance
    private val db = FirebaseFirestore.getInstance()

    // RecyclerView for displaying recent chats
    private lateinit var recyclerView: RecyclerView

    // List to hold recent chat data
    private val recentChatDataList = mutableListOf<RecentChatData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout and set it as the content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up window insets for the layout to avoid content being obscured by system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the toolbar
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        // Initialize the RecyclerView and set its layout manager
        recyclerView = findViewById(R.id.recyclerViewRecentChat)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter for the RecyclerView
        val adapter = RecentChatAdapter(this, recentChatDataList)
        recyclerView.adapter = adapter

        // Set up Firestore listener to fetch chats
        db.collection("chats")
            .whereArrayContains("participants", auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d("MainActivity", "Error fetching chats: ${error.message}")
                    return@addSnapshotListener
                }

                // Process document changes in the snapshot
                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED -> updateRecentChatData(change)
                            DocumentChange.Type.MODIFIED -> updateRecentChatData(change)
                            DocumentChange.Type.REMOVED -> {
                                // Handle chat removal if needed
                            }
                        }
                    }
                }
            }

        // Set up the search button click listener
        binding.floatingActionButtonSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    // Update the recent chat data based on document changes
    private fun updateRecentChatData(change: DocumentChange) {
        // Extract chat details from the document
        val lastMessage = change.document.getString("lastMessage") ?: ""
        val lastMessageTimestamp = change.document.getTimestamp("lastMessageTimestamp") ?: Timestamp.now()
        val formattedTimestamp = getRelativeTime(lastMessageTimestamp)

        // Get participants of the chat and identify the current user and the other participant
        val participants = change.document.get("participants") as List<String>
        val (thisParticipant, otherParticipant) = if (participants[0] == auth.currentUser?.uid) {
            participants[0] to participants[1]
        } else {
            participants[1] to participants[0]
        }

        // Fetch user details of both participants individually
        val userDetailsTasks = listOf(
            db.collection("users").document(thisParticipant).get(),
            db.collection("users").document(otherParticipant).get()
        )

        Tasks.whenAllComplete(userDetailsTasks).addOnCompleteListener { taskList ->
            if (taskList.isSuccessful) {
                val currentUserDetails = userDetailsTasks[0].result
                val otherUserDetails = userDetailsTasks[1].result

                // Ensure both users exist
                if (currentUserDetails != null && otherUserDetails != null) {
                    val displayName = otherUserDetails.getString("displayName") ?: ""
                    val avatar = otherUserDetails.getString("displayImage") ?: ""
                    val userName = otherUserDetails.getString("username") ?: ""
                    val lastMessageSender = currentUserDetails.getString("username") ?: ""

                    // Create a RecentChatData object with the fetched data
                    val recentChatData = RecentChatData(
                        id = otherParticipant,
                        userName = userName,
                        displayName = displayName,
                        avatar = avatar,
                        lastMessage = lastMessage,
                        lastMessageSender = lastMessageSender,
                        lastMessageTimeStamp = formattedTimestamp
                    )

                    // Update the recent chat data list based on the document change type
                    when (change.type) {
                        DocumentChange.Type.ADDED -> recentChatDataList.add(recentChatData)
                        DocumentChange.Type.MODIFIED -> {
                            recentChatDataList.clear() // Clear previous data if modified
                            recentChatDataList.add(recentChatData)
                        }

                        DocumentChange.Type.REMOVED -> {}
                    }

                    // Notify the adapter that data has changed
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            } else {
                Log.e("MainActivity", "Error fetching user details: ${taskList.exception?.message}")
            }
        }
    }


    // Inflate the menu for sign-out options
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_signout, menu)
        return true
    }

    // Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.signOut) {
            auth.signOut() // Sign out the user
            startActivity(Intent(this, SignInActivity::class.java)) // Redirect to sign-in activity
            finish() // Close current activity
        }
        return super.onOptionsItemSelected(item)
    }

    // Get a relative time string based on the timestamp
    private fun getRelativeTime(timestamp: Timestamp): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp.seconds * 1000 }
        val today = Calendar.getInstance()

        // Formatters for time and date display
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        return when {
            // If the date is today, return the time
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                timeFormatter.format(calendar.time)
            }
            // If the date is yesterday, return "Yesterday"
            today.apply { add(Calendar.DAY_OF_YEAR, -1) }.let {
                calendar.get(Calendar.YEAR) == it.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == it.get(Calendar.DAY_OF_YEAR)
            } -> {
                "Yesterday"
            }
            // Otherwise, return the date
            else -> {
                dateFormatter.format(calendar.time)
            }
        }
    }
}
