package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.R
import com.example.chatease.databinding.ActivityMainBinding
import com.example.chatease.dataclass.RecentChatData
import com.example.chatease.recyclerview_adapters.RecentChatAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

//        supportActionBar.title =

//        recyclerView = binding.recyclerViewRecentChat
//
//        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
//
//        val recentChatDataList = mutableListOf<RecentChatData>()
//
//        val adapter = RecentChatAdapter(this@MainActivity,recentChatDataList)
//
//        recyclerView.adapter = adapter
//
//        db.collection("chats")
//            .whereArrayContains("participants", auth.currentUser!!.uid)
//            .addSnapshotListener { snapshot, error ->
//                if (error != null) {
//                    Log.d("test", error.toString())
//                    return@addSnapshotListener
//                } else if (snapshot != null && !snapshot.isEmpty) {
//                    for (recentChat in snapshot.documentChanges) {
//                        when (recentChat.type) {
//                            DocumentChange.Type.ADDED -> {
//                                val lastMessage = recentChat.document.getString("lastmessage") ?: ""
//                                val lastMessageTimeStamp =
//                                    recentChat.document.getTimestamp("lastmessagetimestamp")?.toDate() ?: Date()
//
//                                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
//                                formatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
//                                val formattedTimeStamp = formatter.format(lastMessageTimeStamp)
//
//                                val user1 = recentChat.document.get("user_1_details") as Map<String, Any?>
//                                val user2 = recentChat.document.get("user_2_details") as Map<String, Any?>
//
//                                var displayName = ""
//                                var avatar = ""
//
//                                if (user1["userid"] == auth.currentUser?.uid) {
//                                    displayName = user2["displayname"] as String ?: ""
//                                    avatar = user2["avatar"] as String ?: ""
//                                } else {
//                                    displayName = user1["displayname"] as String ?: ""
//                                    avatar = user1["avatar"] as String ?: ""
//                                }
//
//                                val recentChatDataObj = RecentChatData(displayName,avatar,lastMessage,formattedTimeStamp)
//                                recentChatDataList.add(recentChatDataObj)
//                                adapter.notifyDataSetChanged()
//                            }
//
//                            DocumentChange.Type.MODIFIED -> {}
//                            DocumentChange.Type.REMOVED -> {}
//                        }
//                    }

        binding.floatingActionButtonSearch.setOnClickListener {
            startActivity(Intent(this@MainActivity, SearchActivity::class.java))
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_signout, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == R.id.signOut) {
//            auth.signOut()
//            startActivity(Intent(this@MainActivity, SignInActivity::class.java))
//            finish()
//        }
//        return super.onOptionsItemSelected(item)
//    }
}
