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

class ChatActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityChatBinding
    lateinit var recyclerView: RecyclerView
    var messagesList = mutableListOf<MessageUserData>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Taking Custom Toolbar from view binding
        val toolbar = binding.chatActivityToolbar
        // adding custom toolbar to actual view
        setSupportActionBar(toolbar)
        //enables back button on toolbar to get back to parent activity defined under Manifest File
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //to disable the default app name on ActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.textViewDisplayName.text = intent.getStringExtra("username")
        Picasso.get().load(intent.getStringExtra("avatar")).into(binding.roundedImageViewDisplayImage)


        val currentUserId = auth.currentUser?.uid
        val conversationID = generateConversationID(currentUserId!!,intent.getStringExtra("id")!!)

        binding.buttonSend.setOnClickListener {
            if (binding.editTextMessage.text.toString().trim().isNotEmpty()) {
                val messageRef = db.collection("chats").document(conversationID).collection("messages")
                val newMessageId = messageRef.document().id // Firebase generates a random ID

                val messageData = hashMapOf(
                    "sender" to auth.currentUser?.uid,
                    "content" to binding.editTextMessage.text.toString().trim(),
                    "timestamp" to FieldValue.serverTimestamp()
                )

                messageRef.document(newMessageId).set(messageData)
                    .addOnCompleteListener{ task ->
                        if(task.isSuccessful) {
                            binding.editTextMessage.text.clear()
                        }

                    }
                    .addOnFailureListener { exception ->
                    Toast.makeText(this@ChatActivity, exception.toString(), Toast.LENGTH_LONG).show()
                }
            }
        }

        recyclerView = binding.recyclerViewCurrentChat
        recyclerView.layoutManager = LinearLayoutManager(this@ChatActivity)
        val adapter = ChatAdapter(messagesList,currentUserId!!)
        recyclerView.adapter = adapter

        db.collection("chats").document(conversationID).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                Log.d("test",snapshot.toString())
                Log.e("test",error.toString())
                if (error != null) {
                    Log.d("LoadMessages", error.toString())
                    return@addSnapshotListener
                } else if (snapshot != null && !snapshot.isEmpty) {
                    for (message in snapshot.documentChanges) {
                        when (message.type) {
                            DocumentChange.Type.ADDED -> {
                                val sender = message.document.getString("sender") ?: ""
                                val content = message.document.getString("content") ?: ""
                                val timestamp = (message.document.get("timestamp") as? Date)?.time ?: 0L
                                val date = Date(timestamp)
                                val formatter = SimpleDateFormat("hh:mm a",Locale.getDefault())
                                val formattedTimeStamp = formatter.format(date)
                                val messageObject = MessageUserData(sender,content,formattedTimeStamp)
                                messagesList.add(messageObject)
                                adapter.notifyDataSetChanged()
                                recyclerView.scrollToPosition(messagesList.size - 1)
                            }

                            DocumentChange.Type.MODIFIED -> {

                            }

                            DocumentChange.Type.REMOVED -> {

                            }
                        }
                    }
                }
            }




    }


    fun generateConversationID(user1: String,user2:String) : String {
        val convoId = listOf(user1,user2).sorted()
        return convoId.joinToString("_")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { // adding drop down menu option to the chat layout
        menuInflater.inflate(R.menu.menu_chat_options, menu)
        return true
    }
}

