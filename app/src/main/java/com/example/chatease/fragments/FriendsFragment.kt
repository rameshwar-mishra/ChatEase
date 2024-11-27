package com.example.chatease.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.example.chatease.R
import com.example.chatease.activities.FriendRequestsActivity
import com.example.chatease.adapters_recyclerview.FriendsUserAdapter
import com.example.chatease.databinding.FragmentFriendsBinding
import com.example.chatease.dataclass.SearchUserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendsFragment : Fragment() {
    private lateinit var binding: FragmentFriendsBinding
    private val rtDB = FirebaseDatabase.getInstance()
    private lateinit var listenerObject: ChildEventListener
    private var userDataList = mutableListOf<SearchUserData>()
    private lateinit var adapter: FriendsUserAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        adapter = FriendsUserAdapter(context = requireContext(), userData = userDataList)
        binding.recyclerViewFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFriends.adapter = adapter

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                Log.d("background", "1")
                updateBackground()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                Log.d("background", "2")
                updateBackground()
            }

            private fun updateBackground() {
                Log.d("background", "3")
                Log.d("background item count", adapter.itemCount.toString())
                if (adapter.itemCount == 0) {
                    Log.d("background", "4")
                    binding.recyclerViewFriends.setBackgroundColor(Color.TRANSPARENT)
                } else {
                    Log.d("background", "5")
                    binding.recyclerViewFriends.background = ContextCompat.getDrawable(requireContext(), R.drawable.shape_friendlist)
                }
            }
        })

        binding.cardViewFriendRequest.setOnClickListener {
            startActivity(Intent(requireContext(), FriendRequestsActivity::class.java))
        }

        setUpFriendsListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disconnectFriendsListener()
    }

    private fun setUpFriendsListener() {
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                listenerObject = object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        if (snapshot.exists()) {
                            fetchUserProfileData(userID = snapshot.key!!)
                        }
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Log.d("userData Size", userDataList.size.toString())
                            Log.d("userData Search", snapshot.key.toString())
                            Log.d("userData", userDataList.toString())
                            for (index in 0..<userDataList.size) {
                                Log.d("userData Index", index.toString())
                                if (userDataList[index].userID == snapshot.key) {
                                    userDataList.removeAt(index)
                                    adapter.notifyItemChanged(index)
                                    break
                                }
                            }
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                    override fun onCancelled(error: DatabaseError) {}

                }
                rtDB.getReference("users/${currentUser.uid}/friends/requestAccepted")
                    .addChildEventListener(listenerObject)
            }
        }

    }

    private fun disconnectFriendsListener() {
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            listenerObject?.let {
                rtDB.getReference("users/${currentUser.uid}/friends/requestAccepted").removeEventListener(listenerObject)
            }
        }
    }

    private fun fetchUserProfileData(userID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            rtDB.getReference("users/$userID").get().addOnSuccessListener { snapshot ->
                val userProfile = SearchUserData(
                    userID = userID, // Get user ID
                    userName = snapshot.child("userName").getValue(String::class.java) ?: "", // Get username
                    displayName = snapshot.child("displayName").getValue(String::class.java) ?: "", // Get display name
                    userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: "" // Get user avatar
                )
                userDataList.add(userProfile)
                adapter.notifyItemInserted(userDataList.size - 1)
            }
        }
    }
}