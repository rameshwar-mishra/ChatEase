package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupParticipantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        adapter = SelectGroupParticipantsAdapter(context = this@GroupParticipantsActivity, userDataList = userDataList)

        binding.recyclerView.layoutManager = LinearLayoutManager(this@GroupParticipantsActivity)
        binding.recyclerView.adapter = adapter
        var addedBackground = false
        adapterDataObserver =  object : AdapterDataObserver() {
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
                val intent = Intent(this@GroupParticipantsActivity, GroupCreationActivity::class.java)
                intent.putStringArrayListExtra("selectedParticipants", ArrayList(adapter.getSelectedParticipantsSet()))
//                intent.putExtra("userDataList",ArrayList(userDataList))
                startActivity(intent)
            } else {
                Toast.makeText(this@GroupParticipantsActivity, "You need to select atleast 1 participant", Toast.LENGTH_LONG)
                    .show()
            }
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
                                getUserDetails(id)
                            }
                        }
                    }
                }
        }
    }

    private fun getUserDetails(userID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            rtDB.getReference("users/$userID").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        userDataList.add(
                            UserData(
                                userName = snapshot.child("userName").getValue(String::class.java) ?: "",
                                displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                                userID = userID,
                                userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: ""
                            )
                        )
                        adapter.notifyItemInserted(userDataList.size - 1)
                    }
                }
        }
    }
}