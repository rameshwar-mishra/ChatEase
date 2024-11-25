package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.databinding.ActivityUserProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserProfileActivity : AppCompatActivity() {
    private val rtDb = FirebaseDatabase.getInstance()
    private lateinit var listenerRequestSentObject: ValueEventListener
    private lateinit var listenerRequestReceivedObject: ValueEventListener
    private lateinit var listenerRequestAcceptedObject: ValueEventListener
    private var otherUserId: String = "" // Variable to store the user ID
    private var displayName = ""
    private lateinit var binding: ActivityUserProfileBinding // View binding for UserProfileActivity layout
    private var addFriendButtonStatus = ""
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
        supportActionBar?.setDisplayShowTitleEnabled(false) // Disable the title to be visible in the toolbar

        otherUserId = intent.getStringExtra("id") ?: "" // Get the user ID from the intent extras

        // Fetch user data from Firestore using the user ID

        fetchOtherUserDetails() // To populate the user profile activity with its data

        val userFromChatActivity =
            intent.getBooleanExtra("userFromChatActivity", false) // Check if user came from ChatActivity

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


        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            setAddFriendButton(currentUser.uid, otherUserId)
            addFriendStatusListener(currentUser.uid)
        }

        binding.addFriendButton.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                when (addFriendButtonStatus) {
                    "Add Friend" -> updateFriendRequest(
                        currentUserID = currentUser.uid,
                        otherUserID = otherUserId,
                        value = true,
                        isDeclined = false,
                        selfDeclined = false
                    )

                    "Request Sent" -> actionOnPressingTheRequestSentButton(
                        currentUserID = currentUser.uid,
                        otherUserID = otherUserId
                    )

                    "Friends" -> actionOnPressingTheFriendsButton(currentUserID = currentUser.uid, otherUserID = otherUserId)
                }
            }
        }

        binding.friendRequestAcceptButton.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                CoroutineScope(Dispatchers.IO).launch {
                    whenUserAcceptsFriendRequest(currentUserID = currentUser.uid, otherUserID = otherUserId)
                }
            }
        }

        binding.friendRequestDeclineButton.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                CoroutineScope(Dispatchers.IO).launch {
                    whenUserDeclinesFriendRequest(currentUserID = currentUser.uid, otherUserID = otherUserId)
                }
            }
        }
    }

    private fun fetchOtherUserDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            rtDb.getReference("users").child(otherUserId).get().addOnCompleteListener { task ->
                if (task.isSuccessful) { // Check if Firestore data retrieval was successful
                    val userName = task.result.child("userName").getValue(String::class.java) ?: "" // Get
                    displayName = task.result.child("displayName").getValue(String::class.java) ?: ""
                    val userAvatar = task.result.child("avatar").getValue(String::class.java) ?: "" // Get
                    val userBio = task.result.child("userBio").getValue(String::class.java) ?: "" // Get e

                    CoroutineScope(Dispatchers.Main).launch {
                        // Load avatar image into ImageView using Glide, with a default placeholder
                        if (!isDestroyed && !isFinishing) {
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
            }
        }
    }


    private fun addFriendStatusListener(currentUserID: String) {
        requestSentListener(currentUserID = currentUserID)
        requestReceived(currentUserID = currentUserID)
    }

    private fun requestSentListener(currentUserID: String) {
        listenerRequestSentObject = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.addFriendButton.text = "Request Sent"
                    addFriendButtonStatus = "Request Sent"
                } else {
                    requestAccepted(currentUserID = currentUserID)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        rtDb.getReference("users/$currentUserID/friends/requestSent/$otherUserId")
            .addValueEventListener(listenerRequestSentObject)
    }

    private fun requestReceived(currentUserID: String) {
        listenerRequestReceivedObject = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    cardViewVisibility(status = true)
                } else {
                    cardViewVisibility(status = false)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        rtDb.getReference("users/$currentUserID/friends/requestReceived/$otherUserId")
            .addValueEventListener(listenerRequestReceivedObject)
    }

    private fun requestAccepted(currentUserID: String) {
        listenerRequestAcceptedObject = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.addFriendButton.text = "Friends"
                    addFriendButtonStatus = "Friends"
                } else {
                    binding.addFriendButton.text = "Add Friend"
                    addFriendButtonStatus = "Add Friend"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        rtDb.getReference("users/$currentUserID/friends/requestAccepted/$otherUserId")
            .addValueEventListener(listenerRequestAcceptedObject)
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            rtDb.getReference("users/${currentUser.uid}/friends/requestSent/$otherUserId")
                .removeEventListener(listenerRequestSentObject)
            rtDb.getReference("users/${currentUser.uid}/friends/requestReceived/$otherUserId")
                .removeEventListener(listenerRequestReceivedObject)
            rtDb.getReference("users/${currentUser.uid}/friends/requestAccepted/$otherUserId")
                .removeEventListener(listenerRequestAcceptedObject)
        }
    }

    private fun actionOnPressingTheFriendsButton(currentUserID: String, otherUserID: String) {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("Remove Friend")
            setMessage("Are you sure, you want to remove this user from your friend list?")
            setIcon(R.drawable.vector_icon_friends)
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
            setPositiveButton("Yes") { _, _ ->
                updateRequestAcceptedDB(currentUserID = currentUserID, otherUserID = otherUserID, value = null)
            }
            show()
        }
    }

    private fun updateRequestAcceptedDB(currentUserID: String, otherUserID: String, value: Boolean?) {
        CoroutineScope(Dispatchers.IO).launch {
            if (updateFriendFromOtherUserDB(currentUserID = currentUserID, otherUserID = otherUserID, value = value)) {
                val rtDB = FirebaseDatabase.getInstance()
                if (value != null) {
                    rtDB.getReference("users/$currentUserID/friends/requestAccepted")
                        .updateChildren(
                            mapOf(
                                otherUserID to value
                            )
                        )
                } else {
                    rtDB.getReference("users/$currentUserID/friends/requestAccepted/$otherUserID")
                        .removeValue()
                }

            }
        }
    }

    private suspend fun updateFriendFromOtherUserDB(currentUserID: String, otherUserID: String, value: Boolean?): Boolean {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { coroutine ->
                val rtDB = FirebaseDatabase.getInstance()
                if (value != null) {
                    rtDB.getReference("users/$otherUserID/friends/requestAccepted")
                        .updateChildren(
                            mapOf(
                                currentUserID to value
                            )
                        )
                        .addOnSuccessListener {
                            coroutine.resume(true)
                        }
                        .addOnFailureListener { exception ->
                            coroutine.resumeWithException(exception)
                        }
                } else {
                    rtDB.getReference("users/$otherUserID/friends/requestAccepted/$currentUserID")
                        .removeValue()
                        .addOnSuccessListener {
                            coroutine.resume(true)
                        }
                        .addOnFailureListener { exception ->
                            coroutine.resumeWithException(exception)
                        }
                }

            }
        }
    }

    private fun cardViewVisibility(status: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            if (status && binding.cardViewFriendRequest.visibility == View.GONE) {
                binding.addFriendButton.visibility = View.GONE
                binding.textViewFriendRequestSender.text = "$displayName sent you a friend Request"
                binding.cardViewFriendRequest.visibility = View.VISIBLE
            } else {
                binding.addFriendButton.visibility = View.VISIBLE
                binding.cardViewFriendRequest.visibility = View.GONE
            }
        }
    }

    private fun whenUserAcceptsFriendRequest(currentUserID: String, otherUserID: String) {
        updateRequestAcceptedDB(currentUserID = currentUserID, otherUserID = otherUserID, value = true)
        updateFriendRequest(
            currentUserID = currentUserID,
            otherUserID = otherUserID,
            value = null,
            isDeclined = false,
            selfDeclined = false
        )
        binding.addFriendButton.text = "Friends"
    }

    private fun whenUserDeclinesFriendRequest(currentUserID: String, otherUserID: String) {
        updateFriendRequest(
            currentUserID = currentUserID,
            otherUserID = otherUserID,
            value = null,
            isDeclined = true,
            selfDeclined = false
        )
    }

    private fun actionOnPressingTheRequestSentButton(currentUserID: String, otherUserID: String) {
        MaterialAlertDialogBuilder(this).apply {
            setTitle("Friend Request")
            setMessage("Are you sure, you want to withdraw your friend request?")
            setIcon(R.drawable.vector_icon_friends)
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
            setPositiveButton("Yes") { _, _ ->
                updateFriendRequest(
                    currentUserID = currentUserID,
                    otherUserID = otherUserID,
                    value = null,
                    isDeclined = true,
                    selfDeclined = true
                )
            }
            show()
        }
    }

    private fun updateFriendRequest(
        currentUserID: String,
        otherUserID: String,
        value: Boolean?,
        isDeclined: Boolean,
        selfDeclined: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (updateFriendRequestInOtherUserDB(
                    currentUserID = currentUserID,
                    otherUserID = otherUserID,
                    value = value,
                    selfDeclined = selfDeclined
                )
            ) {
                val rtDB = FirebaseDatabase.getInstance()
                if (value != null) {
                    rtDB.getReference("users/$currentUserID/friends/requestSent")
                        .updateChildren(
                            mapOf(
                                otherUserID to value
                            )
                        )
                } else {
                    if (selfDeclined) {
                        rtDB.getReference("users/$currentUserID/friends/requestSent/$otherUserID")
                            .removeValue()
                    } else {
                        rtDB.getReference("users/$currentUserID/friends/requestReceived/$otherUserID")
                            .removeValue()
                    }

                    if (isDeclined) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Log.d("test", "changed")
                            binding.addFriendButton.text = "Add Friend"
                            addFriendButtonStatus = "Add Friend"
                        }
                    }
                }

            }
        }
    }

    private suspend fun updateFriendRequestInOtherUserDB(
        currentUserID: String,
        otherUserID: String,
        value: Boolean?,
        selfDeclined: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { coroutine ->
                val rtDB = FirebaseDatabase.getInstance()
                if (value != null) {
                    rtDB.getReference("users/$otherUserID/friends/requestReceived")
                        .updateChildren(
                            mapOf(
                                currentUserID to value
                            )
                        )
                        .addOnSuccessListener {
                            coroutine.resume(true)
                        }
                        .addOnFailureListener { exception ->
                            coroutine.resumeWithException(exception)
                        }
                } else {
                    if (selfDeclined) {
                        rtDB.getReference("users/$otherUserID/friends/requestReceived/$currentUserID")
                            .removeValue()
                            .addOnSuccessListener {
                                coroutine.resume(true)
                            }
                            .addOnFailureListener { exception ->
                                coroutine.resumeWithException(exception)
                            }
                    } else {
                        rtDB.getReference("users/$otherUserID/friends/requestSent/$currentUserID")
                            .removeValue()
                            .addOnSuccessListener {
                                coroutine.resume(true)
                            }
                            .addOnFailureListener { exception ->
                                coroutine.resumeWithException(exception)
                            }
                    }
                }

            }
        }
    }

    private fun setAddFriendButton(currentUserID: String, otherUserID: String) {
        CoroutineScope(Dispatchers.Main).launch {
            addFriendButtonStatus = when {
                isRequestSent(currentUserID, otherUserID) -> "Request Sent"
                isRequestReceived(currentUserID, otherUserID) -> "Accept Request"
                isRequestAccepted(currentUserID, otherUserID) -> "Friends"
                else -> "Add Friend"
            }

            if (addFriendButtonStatus != "Accept Request") {
                binding.addFriendButton.text = addFriendButtonStatus
            }
        }
    }

    private suspend fun isRequestSent(currentUserID: String, otherUserID: String): Boolean {
        return checkIfUserExistInTheMap("requestSent", currentUserID, otherUserID)
    }

    private suspend fun isRequestReceived(currentUserID: String, otherUserID: String): Boolean {
        return checkIfUserExistInTheMap("requestReceived", currentUserID, otherUserID)
    }

    private suspend fun isRequestAccepted(currentUserID: String, otherUserID: String): Boolean {
        return checkIfUserExistInTheMap("requestAccepted", currentUserID, otherUserID)
    }

    private suspend fun checkIfUserExistInTheMap(mapType: String, currentUserID: String, otherUserID: String): Boolean {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { coroutine ->
                val rtDB = FirebaseDatabase.getInstance()

                rtDB.getReference("users/$currentUserID/friends/$mapType/$otherUserID")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            coroutine.resume(snapshot.exists())
                        }

                        override fun onCancelled(error: DatabaseError) {
                            coroutine.resumeWithException(error.toException())
                        }
                    })
            }
        }
    }

    private fun onBackStackToChatActivity() {
        val intent = Intent() // Create a new Intent for sending back data
        intent.apply {
            putExtra("id", otherUserId) // Add user ID to the intent
        }
        setResult(RESULT_OK, intent) // Set result for the activity to pass back data
        finish() // Close the current activity
    }
}
