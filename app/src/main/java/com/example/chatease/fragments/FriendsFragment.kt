package com.example.chatease.fragments

import android.content.Intent
import android.os.Bundle
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
import com.example.chatease.dataclass.UserData
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
    private var userDataList = mutableListOf<UserData>()
    private lateinit var adapter: FriendsUserAdapter
    private lateinit var adapterDataObserver: AdapterDataObserver
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
        var addedBackground = false
        adapterDataObserver = object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                updateBackground()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                updateBackground()
            }

            private fun updateBackground() {
                val hasItems = adapter.itemCount > 0
                if (hasItems && !addedBackground) {
                    addedBackground = true
                    binding.recyclerViewFriends.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.card_view_design)
                } else if (!hasItems) {
                    addedBackground = false
                    binding.recyclerViewFriends.background = null
                }
            }
        }

        adapter.registerAdapterDataObserver(adapterDataObserver)

        binding.cardViewFriendRequest.setOnClickListener {
            startActivity(Intent(requireContext(), FriendRequestsActivity::class.java))
        }

        setUpFriendsListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disconnectFriendsListener()
        adapterDataObserver.let {
            adapter.unregisterAdapterDataObserver(adapterDataObserver)
        }
    }

    private fun setUpFriendsListener() {
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                listenerObject = object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        if (snapshot.exists()) {
                            addToUserDataList(userID = snapshot.key!!)
                        }
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (index in userDataList.indices) {
                                if (userDataList[index].userID == snapshot.key) {
                                    userDataList.removeAt(index)
                                    adapter.notifyItemRemoved(index)

                                    binding.textViewFriendsCounter.text = "All Friends - ${userDataList.size}"
//                                    if (userDataList.size == 0) {
////                                        binding.textViewFriendsCounter.visibility = View.GONE
//                                        binding.textViewFriendsCounter.text = "All Friends - ${userDataList.size}"
//                                    } else {
//                                        binding.textViewFriendsCounter.text = "All Friends - ${userDataList.size}"
//                                    }
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

    private val userIdSet = HashSet<String>()

    private fun addToUserDataList(userID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (userIdSet.contains(userID)) {
                return@launch
            } else {
                fetchUserProfileData(userID = userID)
            }
        }
    }

    private fun fetchUserProfileData(userID: String) {
        rtDB.getReference("users/$userID").get().addOnSuccessListener { snapshot ->
            val userProfile = UserData(
                userID = userID, // Get user ID
                userName = snapshot.child("userName").getValue(String::class.java) ?: "", // Get username
                displayName = snapshot.child("displayName").getValue(String::class.java) ?: "", // Get display name
                userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: "" // Get user avatar
            )

            val insertIndex = userDataList.binarySearch { it.displayName.compareTo(userProfile.displayName) }
                .let { returnValue ->
                    if (returnValue < 0) {
                        -returnValue - 1
                    } else {
                        returnValue + 1
                    }
                }

            userDataList.add(insertIndex, userProfile)
            userIdSet.add(userID)
            binding.textViewFriendsCounter.visibility = View.VISIBLE
            binding.textViewFriendsCounter.text = "All Friends - ${userDataList.size}"
            adapter.notifyItemInserted(insertIndex)
        }
    }
}