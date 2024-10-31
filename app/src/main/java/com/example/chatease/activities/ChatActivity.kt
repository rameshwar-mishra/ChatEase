package com.example.chatease.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.recyclerview_adapters.ChatAdapter
import com.example.chatease.dataclass.MessageUserData
import com.example.chatease.R
import com.example.chatease.databinding.ActivityChatBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ChatActivity handles the chat screen functionality
class ChatActivity : AppCompatActivity() {

    // Firestore database instance
    private val db = Firebase.firestore
    // Firebase Authentication instance to handle user authentication
    private val auth = FirebaseAuth.getInstance()
    // View binding for the activity layout
    private lateinit var binding: ActivityChatBinding
    // RecyclerView for displaying messages
    lateinit var recyclerView: RecyclerView
    // List to hold message data
    var messagesList = mutableListOf<MessageUserData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adjusting the padding for the main view to avoid overlap with system UI elements
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // Getting the insets for the system bars (status bar, navigation bar)
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Setting padding based on the insets to avoid content overlap
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Taking Custom Toolbar from view binding
        val toolbar = binding.chatActivityToolbar
        // Setting the custom toolbar as the ActionBar
        setSupportActionBar(toolbar)
        // Enables back button on toolbar to navigate to parent activity
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Disabling the default app name display on the ActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setting the display name from intent extra
        binding.textViewDisplayName.text = intent.getStringExtra("username")
        // Loading the user's avatar image using Picasso library
        Picasso.get().load(intent.getStringExtra("avatar")).into(binding.roundedImageViewDisplayImage)

        // Get the current user's ID
        val currentUserId = auth.currentUser?.uid

        val otherUserId = intent.getStringExtra("id")
        if (otherUserId == null) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Generate a unique conversation ID using the current user ID and the other user's ID
        val conversationID = generateConversationID(currentUserId!!, intent.getStringExtra("id")!!)

        // Setting up the send button click listener
        binding.buttonSend.setOnClickListener {
            // Check if the message input is not empty
            if (binding.editTextMessage.text.toString().trim().isNotEmpty()) {
                // Reference to the metadata document for the chat
                val metaRef = db.collection("chats").document(conversationID)

                // List of participants sorted (current user and the other user)
                val participants = listOf(auth.currentUser!!.uid, intent.getStringExtra("id")!!).sorted()

                // Variables for user display name and avatar
                var userDisplayName = ""
                var userAvatar = ""
                // Timestamp for the message
                val timestamp = FieldValue.serverTimestamp()
                // Last message content
                val lastMessage = binding.editTextMessage.text.toString().trim()

                // Creating a map to store metadata of the chat
                val userMetaData = hashMapOf(
                    "participants" to participants,
                    "lastMessage" to lastMessage,
                    "lastMessageTimestamp" to timestamp,
                    "lastMessageSender" to auth.currentUser!!.uid
                )

                // Setting the metadata document in Firestore
                metaRef.set(userMetaData)

                // Reference to the messages collection within the chat document
                val messageRef = db.collection("chats").document(conversationID).collection("messages")
                // Generating a new message ID
                val newMessageId = messageRef.document().id // Firebase generates a random ID

                // Creating a map for the message data
                val messageData = hashMapOf(
                    "sender" to auth.currentUser?.uid,
                    "content" to binding.editTextMessage.text.toString().trim(),
                    "timestamp" to timestamp
                )

                // Adding the new message to Firestore
                messageRef.document(newMessageId).set(messageData)
                    .addOnCompleteListener { task ->
                        // Clearing the message input field on successful send
                        if (task.isSuccessful) {
                            binding.editTextMessage.text.clear()
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Displaying an error message if sending fails
                        Toast.makeText(this@ChatActivity, exception.toString(), Toast.LENGTH_LONG).show()
                    }
            }
        }

        // Setting up the RecyclerView for displaying messages
        recyclerView = binding.recyclerViewCurrentChat
        recyclerView.layoutManager = LinearLayoutManager(this@ChatActivity)
        val adapter = ChatAdapter(messagesList, currentUserId!!)
        recyclerView.adapter = adapter

        // Listening for changes in the messages collection
        db.collection("chats").document(conversationID).collection("messages")
            .orderBy("timestamp") // Ordering messages by timestamp
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d("LoadMessages", error.toString()) // Logging the error if any
                    return@addSnapshotListener
                } else if (snapshot != null && !snapshot.isEmpty) {
                    // Loop through each document change
                    for (message in snapshot.documentChanges) {
                        when (message.type) {
                            DocumentChange.Type.ADDED -> {
                                // Extracting data from the message document
                                val sender = message.document.getString("sender") ?: ""
                                val content = message.document.getString("content") ?: ""
                                val timestamp = message.document.getTimestamp("timestamp")?.toDate() ?: Date()
                                // Formatting the timestamp to a readable string
                                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                formatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                                val formattedTimeStamp = formatter.format(timestamp)
                                // Creating a MessageUserData object
                                val messageObject = MessageUserData(sender, content, formattedTimeStamp)
                                // Adding the message to the list and notifying the adapter
                                messagesList.add(messageObject)
                                adapter.notifyDataSetChanged()
                                // Scrolling to the bottom to show the latest message
                                recyclerView.scrollToPosition(messagesList.size - 1)
                            }

                            DocumentChange.Type.MODIFIED -> {
                                // Handle modified messages if needed
                            }

                            DocumentChange.Type.REMOVED -> {
                                // Handle removed messages if needed
                            }
                        }
                    }
                }
            }
    }

    // Function to generate a unique conversation ID based on the participants
    fun generateConversationID(user1: String, user2: String): String {
        val convoId = listOf(user1, user2).sorted() // Sorting the user IDs to maintain consistency
        return convoId.joinToString("_") // Joining the sorted IDs to create a unique ID
    }

    // Function to create options menu for the chat layout
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu resource
        menuInflater.inflate(R.menu.menu_chat_options, menu)
        return true
    }
}
