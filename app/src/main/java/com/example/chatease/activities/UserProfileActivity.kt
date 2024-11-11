package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.databinding.ActivityUserProfileBinding
import com.google.firebase.database.FirebaseDatabase

class UserProfileActivity : AppCompatActivity() {
    private val rtDb = FirebaseDatabase.getInstance()
    private var userId: String = "" // Variable to store the user ID
    private lateinit var binding: ActivityUserProfileBinding // View binding for UserProfileActivity layout

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityUserProfileBinding.inflate(layoutInflater) // Inflate the layout using view binding
        super.onCreate(savedInstanceState)
        setContentView(binding.root) // Set the content view to the root of the binding

        // Handle window insets to adjust layout for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.userProfileActivityToolbar // Get reference to toolbar
        setSupportActionBar(toolbar) // Set toolbar as the action bar for the activity
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button in toolbar
        supportActionBar?.title = "User Profile" // Set the title of the toolbar

        userId = intent.getStringExtra("id") ?: "" // Get the user ID from the intent extras

        // Fetch user data from Firestore using the user ID

        rtDb.getReference("users").child(userId).get().addOnCompleteListener { task ->
            if (task.isSuccessful) { // Check if Firestore data retrieval was successful
                val userName = task.result.child("userName").getValue(String::class.java) ?: "" // Get
                val displayName = task.result.child("displayName").getValue(String::class.java) ?: ""
                val userAvatar = task.result.child("avatar").getValue(String::class.java)?: "" // Get
                val userBio = task.result.child("userBio").getValue(String::class.java) ?: "" // Get e

                // Load avatar image into ImageView using Glide, with a default placeholder
                if(!isDestroyed && !isFinishing){
                    Glide.with(this@UserProfileActivity)
                        .load(userAvatar)
                        .placeholder(R.drawable.vector_default_user_avatar)
                        .into(binding.userProfilePic)
                }

                binding.userName.text = "@$userName" // Set username text in UI
                binding.displayName.text = displayName // Set display name text in UI
                binding.textViewBioText.text = userBio // Set user bio text in UI
            }
        }


        val userFromChatActivity = intent.getBooleanExtra("userFromChatActivity", false) // Check if user came from ChatActivity

        // If user came from ChatActivity, hide the message button
        if (userFromChatActivity) {
            binding.messageUserButton.visibility = View.GONE // Hide the message button
        } else {
            // If not from ChatActivity, set click listener on message button
            binding.messageUserButton.setOnClickListener {
                // Handle message button click
            }
        }

        // Set click listener for toolbar navigation button to handle back navigation
        toolbar.setNavigationOnClickListener {
            onBackStackToChatActivity()
        }
    }

    private fun onBackStackToChatActivity() {
        val intent = Intent() // Create a new Intent for sending back data
        intent.apply {
            putExtra("id", userId) // Add user ID to the intent
        }
        setResult(RESULT_OK, intent) // Set result for the activity to pass back data
        finish() // Close the current activity
    }

    // Inflate menu options for user profile
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat_options, menu) // Inflate the menu resource into the toolbar
        return true // Return true to display the menu
    }
}
