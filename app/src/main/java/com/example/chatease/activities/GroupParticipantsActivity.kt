package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.example.chatease.R
import com.example.chatease.adapters_recyclerview.SelectGroupParticipantsAdapter
import com.example.chatease.databinding.ActivityGroupParticipantsBinding
import com.example.chatease.dataclass.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupParticipantsActivity : AppCompatActivity() {
    private val rtDB = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    lateinit var binding: ActivityGroupParticipantsBinding
    private val userDataList = mutableListOf<UserData>()
    private lateinit var adapter: SelectGroupParticipantsAdapter
    private lateinit var adapterDataObserver: AdapterDataObserver
    private var participantStringArrayList: ArrayList<String>? = null
    private var groupID: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        groupID = intent.getStringExtra("groupID")
        participantStringArrayList = intent.getStringArrayListExtra("participantStringArrayList")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            if (participantStringArrayList != null) {
                val intent = Intent(this@GroupParticipantsActivity, GroupProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this@GroupParticipantsActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }

        adapter = SelectGroupParticipantsAdapter(context = this@GroupParticipantsActivity, userDataList = userDataList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this@GroupParticipantsActivity)
        binding.recyclerView.adapter = adapter

        var addedBackground = false
        adapterDataObserver = object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (!addedBackground && adapter.itemCount > 0) {
                    addedBackground = true
                    val params = binding.recyclerView.layoutParams
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    binding.recyclerView.layoutParams = params
                    binding.textViewFriend.visibility = View.VISIBLE
                    binding.recyclerView.background =
                        ContextCompat.getDrawable(this@GroupParticipantsActivity, R.drawable.shape_recyclerview_background)
                }
            }
        }
        adapter.registerAdapterDataObserver(adapterDataObserver)

        auth.currentUser?.let { currentUser ->
            fetchFriends(currentUser.uid)
        }

        binding.floatingActionButtonNext.setOnClickListener {

            if (adapter.getSelectedParticipantsSet().isNotEmpty()) {

                if (participantStringArrayList != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        groupID?.let { groupId ->
                            val participantsMutableSet = adapter.getSelectedParticipantsSet()
                            val participantsMap: MutableMap<String, Any> = mutableMapOf()
                            participantsMutableSet.forEach { participant ->
                                participantsMap[participant] = true
                            }

                            rtDB.getReference("groups/$groupId/metadata/participants").updateChildren(participantsMap)
                                .addOnSuccessListener {

                                    if (adapter.getSelectedParticipantsSet().size > 1) {
                                        Toast.makeText(
                                            this@GroupParticipantsActivity,
                                            "Added ${participantsMutableSet.size - 1} participants",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this@GroupParticipantsActivity,
                                            "Added 1 participant",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    val intent = Intent() // Create a new Intent for sending back data
                                    intent.apply {
                                        putExtra("updateParticipantList", true) // Add user ID to the intent
                                    }
                                    setResult(RESULT_OK, intent) // Set result for the activity to pass back data
                                    finish() // Close the current activity
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@GroupParticipantsActivity, "Something went wrong, please try again", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                } else {
                    Log.v("participant", adapter.getSelectedParticipantsSet().toString())
                    val intent = Intent(this@GroupParticipantsActivity, GroupCreationActivity::class.java)
                    intent.putStringArrayListExtra("selectedParticipants", ArrayList(adapter.getSelectedParticipantsSet()))
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this@GroupParticipantsActivity, "You need to select atleast 1 participant", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (participantStringArrayList != null) {
            val intent = Intent(this@GroupParticipantsActivity, GroupProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adapterDataObserver.let {
            adapter.unregisterAdapterDataObserver(adapterDataObserver)
        }
    }

    private fun fetchFriends(currentUserID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            rtDB.getReference("users/$currentUserID/friends/requestAccepted").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.children.forEach { user ->
                            user.key?.let { id ->
                                if (participantStringArrayList != null) {
                                    if (!participantStringArrayList!!.contains(id)) {
                                        getUserDetails(id)
                                    }
                                } else {
                                    getUserDetails(id)
                                }
                            }
                        }
                    }
                }
        }
    }

    private val delayHandler = Handler(Looper.getMainLooper())
    private fun getUserDetails(userID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            rtDB.getReference("users/$userID").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val userData = UserData(
                            userName = snapshot.child("userName").getValue(String::class.java) ?: "",
                            displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                            userID = userID,
                            userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: ""
                        )

                        val insertIndex = userDataList.binarySearch { it.displayName.compareTo(userData.displayName) }
                            .let { returnValue ->
                                if (returnValue < 0) {
                                    -returnValue - 1
                                } else {
                                    returnValue + 1
                                }
                            }

                        userDataList.add(insertIndex, userData)
                        CoroutineScope(Dispatchers.Main).launch {
                            delayHandler.removeCallbacksAndMessages(null)

                            delayHandler.postDelayed({

                                if (userDataList.size > 0) {
                                    binding.recyclerView.background =
                                        ContextCompat.getDrawable(this@GroupParticipantsActivity, R.drawable.card_view_design)
                                    adapter.notifyDataSetChanged()
                                }
                            }, 100L)
                        }
                    }
                }
        }
    }
}