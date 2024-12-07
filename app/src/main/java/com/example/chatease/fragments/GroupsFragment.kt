package com.example.chatease.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatease.activities.GroupParticipantsActivity
import com.example.chatease.adapters_recyclerview.RecentGroupChatAdapter
import com.example.chatease.databinding.FragmentGroupsBinding
import com.example.chatease.dataclass.RecentGroupChatData
import com.example.chatease.trackers.AppStatusTracker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class GroupsFragment : Fragment() {
    private lateinit var binding: FragmentGroupsBinding
    private val rtDB = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: RecentGroupChatAdapter
    private var groupList = mutableListOf<RecentGroupChatData>()
    private val groupIDSet = mutableSetOf<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.floatingActionButtonCreateGroup.setOnClickListener {
            startActivity(Intent(requireContext(), GroupParticipantsActivity::class.java))
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecentGroupChatAdapter(context = requireContext(), groupList = groupList)
        binding.recyclerView.adapter = adapter

        val appStatusTracker = requireActivity().application as AppStatusTracker

        appStatusTracker.getGroupLiveData().observe(viewLifecycleOwner) { updatedList ->
            Log.d("call","updatedList")
            groupList.clear()
            groupList.addAll(updatedList)
            adapter.notifyDataSetChanged()
        }
//        auth.currentUser?.let { currentUser ->
//            groupListener(currentUserID = currentUser.uid)
//        }
    }

}