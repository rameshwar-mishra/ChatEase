package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.adapters_recyclerview.GroupParticipantsAdapter
import com.example.chatease.databinding.ActivityGroupProfileBinding
import com.example.chatease.dataclass.GroupProfileParticipantsData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GroupProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupProfileBinding
    private val rtDB = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val participantList = mutableListOf<GroupProfileParticipantsData>()
    private lateinit var adapter: GroupParticipantsAdapter
    private var groupID: String? = null
    private val delayHandler = Handler(Looper.getMainLooper())
    private var groupIcon: String = ""
    private var groupName: String = ""
    private var groupDesc: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        groupID = intent.getStringExtra("groupID")

        setSupportActionBar(binding.groupProfileActivityToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (groupID.isNullOrEmpty()) {
            Toast.makeText(this@GroupProfileActivity, "GroupID Not Found", Toast.LENGTH_LONG).show()
            return
        }

        adapter =
            GroupParticipantsAdapter(this@GroupProfileActivity, participantList = participantList)
        binding.recyclerViewParticipantsList.layoutManager =
            LinearLayoutManager(this@GroupProfileActivity)
        binding.recyclerViewParticipantsList.adapter = adapter

        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            CoroutineScope(Dispatchers.IO).launch {
                updateGroupIcon()
                updateGroupName()
                updateGroupCreatedAt()
                updateGroupDescription()
                updateParticipantList(currentUserID = currentUser.uid)
            }

            binding.frameAddParticipants.setOnClickListener {
                addParticipants(currentUserID = currentUser.uid)
            }

            binding.frameLeaveGroup.setOnClickListener {
                leaveGroup()
            }

            binding.frameDeleteGroup.setOnClickListener {
                deleteGroup(currentUserID = currentUser.uid)
            }
        }
    }

    private var menuVisibilityStatus = false
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Log.w("menu_visible 0", menu?.findItem(R.id.settingsIcon)?.isVisible.toString() + " " + menuVisibilityStatus.toString())
        menu?.findItem(R.id.settingsIcon)?.isVisible = menuVisibilityStatus
        Log.w("menu_visible 2", menu?.findItem(R.id.settingsIcon)?.isVisible.toString())
        return super.onPrepareOptionsMenu(menu)
    }

    private fun menuVisibility(boolean: Boolean) {
        Log.w("menu_visible", boolean.toString())
        menuVisibilityStatus = boolean
        invalidateOptionsMenu()
    }

    private fun addParticipants(currentUserID: String) {
        if (participantList.any { participant -> participant.userID == currentUserID && participant.role != "member" }) {
            val participantStringArrayList = participantList.map { it.userID }
            val intent = Intent(this@GroupProfileActivity, GroupParticipantsActivity::class.java)
            intent.apply {
                putExtra("groupID", groupID) // Pass the other user's ID to the profile activity
                putStringArrayListExtra(
                    "participantStringArrayList",
                    participantStringArrayList as ArrayList<String>
                )
            }
            startActivityForResult(intent, 2)
        } else {
            Toast.makeText(
                this@GroupProfileActivity,
                "You are not allowed to add participants as you are not an admin/owner of this group",
                Toast.LENGTH_LONG
            ).show()
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == RESULT_OK) {
            val modifiedParticipantList = data?.getBooleanExtra("updateParticipantList", false)
            modifiedParticipantList?.let {
                if (modifiedParticipantList) {
                    FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                        updateParticipantList(currentUserID = currentUser.uid)
                    }
                }
            }
        } else if (requestCode == 5 && resultCode == RESULT_OK) {
            val modifiedGroupIcon = data?.getStringExtra("modifiedGroupIcon")
            val modifiedGroupName = data?.getStringExtra("modifiedGroupName")
            val modifiedGroupDesc = data?.getStringExtra("modifiedGroupDesc")

            modifiedGroupIcon?.let { value ->
                groupIcon = value
                if (!isFinishing && !isDestroyed) {
                    Glide.with(this)
                        .load(value)
                        .placeholder(R.drawable.vector_icon_group)
                        .into(binding.imageViewGroupIcon)
                }
            }

            modifiedGroupName?.let { value ->
                groupName = value
                binding.textViewGroupName.text = value
            }

            modifiedGroupDesc?.let { value ->
                groupDesc = value
                binding.textViewGroupDesc.text = value
            }
        }
    }

    // Function to create options menu for the chat layout
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu resource
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settingsIcon -> {
                val intent = Intent(this@GroupProfileActivity, GroupSettingsActivity::class.java)
                intent.putExtra("groupID", groupID)
                intent.putExtra("groupIcon", groupIcon)
                intent.putExtra("groupName", groupName)
                intent.putExtra("groupDesc", groupDesc)
                startActivityForResult(intent, 5)
            }

            android.R.id.home -> {
                val intent = Intent()
                intent.putExtra("groupID", groupID)
                intent.putExtra("modifiedGroupIcon", groupIcon)
                intent.putExtra("modifiedGroupName", groupName)
                setResult(RESULT_OK,intent)
                finish()
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun updateGroupIcon() {
        groupID?.let {
            rtDB.getReference("groups/$groupID/metadata/groupIcon").get()
                .addOnSuccessListener { snapshot ->
                    groupIcon = snapshot.value as? String ?: ""
                    if (groupIcon.isNotEmpty()) {
                        Glide.with(this)
                            .load(snapshot.value as? String ?: "")
                            .placeholder(R.drawable.vector_icon_group)
                            .into(binding.imageViewGroupIcon)

                        val params = binding.imageViewGroupIcon.layoutParams
                        params.height = ViewGroup.LayoutParams.MATCH_PARENT
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT
                        binding.imageViewGroupIcon.layoutParams = params
                    }
                }
        }
    }

    private fun updateGroupName() {
        groupID?.let {
            rtDB.getReference("groups/$groupID/metadata/groupName").get()
                .addOnSuccessListener { snapshot ->
                    binding.textViewGroupName.text = snapshot.value as? String ?: ""
                    groupName = snapshot.value as? String ?: ""
                }
        }
    }

    private fun updateGroupCreatedAt() {
        groupID?.let {
            rtDB.getReference("groups/$groupID/metadata").get()
                .addOnSuccessListener { snapshot ->
                    val time = snapshot.child("createdAt").value as? Long ?: 0L
                    val groupOwnerName = snapshot.child("groupOwner").value as? String ?: ""
                    val formattedTime = getRelativeTime(Timestamp((time / 1000), 0))
                    binding.textViewCreatedOn.text = "Created by $groupOwnerName on $formattedTime"
                }
        }
    }

    private fun updateGroupDescription() {
        groupID?.let {
            rtDB.getReference("groups/$groupID/metadata/groupDesc").get()
                .addOnSuccessListener { snapshot ->
                    binding.textViewGroupDesc.text = snapshot.value as? String
                        ?: "Big talks or small talks, ChatEase handles it all."
                    groupDesc = snapshot.value as? String ?: "Big talks or small talks, ChatEase handles it all."
                }
        }
    }

    private fun updateParticipantList(currentUserID: String) {
        groupID?.let {
            rtDB.getReference("groups/$groupID/metadata/participants").get()
                .addOnSuccessListener { snapshot ->
                    for (participant in snapshot.children) {
                        participant.key?.let {
                            Log.w("called", "fetchUserData : $it")
                            fetchUserData(
                                currentUserID = currentUserID,
                                participantID = it,
                                role = participant.child("role").getValue(String::class.java)
                            )
                        }
                    }
                }
        }
    }

    private val participantIDHastSet = HashSet<String>()
    private val participantsHierarchyList = mutableListOf<GroupProfileParticipantsData>()
    private val lexicographicallySortedList = mutableListOf<GroupProfileParticipantsData>()
    private val currentUserInParticipantList = mutableListOf<GroupProfileParticipantsData>()

    private fun fetchUserData(currentUserID: String, participantID: String, role: String? = null) {
        rtDB.getReference("users/$participantID").get()
            .addOnSuccessListener { snapshot ->
                val userData = GroupProfileParticipantsData(
                    userName = snapshot.child("userName").getValue(String::class.java) ?: "",
                    displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                    userID = participantID,
                    userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: "",
                    role = role ?: "member"
                )

                if (!participantIDHastSet.contains(userData.userID)) {
                    participantIDHastSet.add(userData.userID)

                    if (userData.userID == currentUserID) {
                        when (userData.role) {
                            "owner" -> {
                                binding.frameLeaveGroup.visibility = View.GONE
                                binding.frameDeleteGroup.visibility = View.VISIBLE
                                menuVisibility(true)
                            }

                            "admin" -> {
                                //delete group button will be GONE by default
                                //leave group button will be VISIBLE by default
                                menuVisibility(true)
                            }

                            else -> {
                                binding.frameAddParticipants.visibility = View.GONE
                                //delete group button will be Gone
                            }
                        }
                    }


                    when (userData.role) {
                        "owner" -> {
                            participantsHierarchyList.add(index = 0, userData)
                        }

                        "admin" -> {
                            if (userData.userID == currentUserID) {
                                currentUserInParticipantList.add(userData)
                            } else {
                                participantsHierarchyList.add(
                                    index = participantsHierarchyList.size,
                                    userData
                                )
                            }
                        }

                        else -> {
                            if (userData.userID == currentUserID) {
                                currentUserInParticipantList.add(userData)
                            } else {
                                Log.w(
                                    "userData lexicographicallySortedList",
                                    lexicographicallySortedList.toString()
                                )
                                val insertIndex = lexicographicallySortedList.binarySearch {
                                    it.displayName.compareTo(userData.displayName)
                                }
                                    .let { returnValue ->
                                        if (returnValue < 0) {
                                            -returnValue - 1
                                        } else {
                                            returnValue + 1
                                        }
                                    }
                                lexicographicallySortedList.add(insertIndex, userData)
                                Log.v(
                                    "userData Insertion",
                                    "lexicographicallySortedList.add : $insertIndex"
                                )
                            }
                        }
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        delayHandler.removeCallbacksAndMessages(null)

                        delayHandler.postDelayed({
                            if (participantsHierarchyList.isNotEmpty()) {
                                if (currentUserInParticipantList.isNotEmpty()) {
                                    participantList.addAll(currentUserInParticipantList)
                                    participantList.addAll(participantsHierarchyList)
                                    participantList.addAll(lexicographicallySortedList)
                                    currentUserInParticipantList.clear()
                                    participantsHierarchyList.clear()
                                    lexicographicallySortedList.clear()
                                } else {
                                    participantList.addAll(participantsHierarchyList)
                                    participantList.addAll(lexicographicallySortedList)
                                    participantsHierarchyList.clear()
                                    lexicographicallySortedList.clear()
                                }
                            } else {
                                if (currentUserInParticipantList.isNotEmpty()) {
                                    participantList.addAll(currentUserInParticipantList)
                                    participantList.addAll(lexicographicallySortedList)
                                    currentUserInParticipantList.clear()
                                    lexicographicallySortedList.clear()
                                } else {
                                    participantList.addAll(lexicographicallySortedList)
                                    lexicographicallySortedList.clear()
                                }
                            }
                            Log.v("userData UPDATED & CLEARED", "UPDATED & CLEARED")
                            adapter.notifyDataSetChanged()
                            binding.textViewParticipantsCounter.text =
                                "Participants - ${participantList.size}"
                        }, 500L)
                    }
                }
            }
    }

    private fun leaveGroup() {
        groupID?.let {
            MaterialAlertDialogBuilder(this@GroupProfileActivity).apply {
                setTitle("Leave Group")
                setMessage("Are you sure, you want to leave this group? This action is irreversible.")
                setIcon(R.drawable.vector_icon_danger_warning)
                setPositiveButton("Yes") { dialog, _ ->
                    updateDatabaseToLeaveGroup()
                    dialog.cancel()
                }
                setNegativeButton("No") { dialog, _ ->
                    dialog.cancel()
                }
            }.show()
        }
    }

    private fun updateDatabaseToLeaveGroup() {
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            CoroutineScope(Dispatchers.IO).launch {
                rtDB.getReference("groups/$groupID/metadata/participants/${currentUser.uid}")
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@GroupProfileActivity,
                            "Left the group",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@GroupProfileActivity, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this@GroupProfileActivity,
                            "Something went wrong, Failed to leave the group",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
            }
        }
    }

    private fun deleteGroup(currentUserID: String) {
        if (participantList.any { participant -> participant.userID == currentUserID && participant.role == "owner" }) {
            groupID?.let {
                MaterialAlertDialogBuilder(this@GroupProfileActivity).apply {
                    setTitle("Delete Group")
                    setMessage("Are you sure, you want to delete this group? This action is irreversible.")
                    setIcon(R.drawable.vector_icon_danger_warning)
                    setPositiveButton("Yes") { dialog, _ ->
                        updateDatabaseToDeleteGroup()
                        dialog.cancel()
                    }
                    setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }
                }.show()
            }
        } else {
            Toast.makeText(
                this@GroupProfileActivity,
                "You are not allowed to delete this group as you are not an admin/owner of this group",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateDatabaseToDeleteGroup() {
        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
            CoroutineScope(Dispatchers.IO).launch {
                rtDB.getReference("groups/$groupID").removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@GroupProfileActivity,
                            "Deleted the group",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@GroupProfileActivity, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this@GroupProfileActivity,
                            "Something went wrong, Failed to delete the group",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
            }
        }
    }


    // Get a relative time string based on the timestamp for display
    private fun getRelativeTime(timestamp: Timestamp): String {
        // Create calendar instance from the timestamp
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp.seconds * 1000 }
        val today = Calendar.getInstance() // Get current date

        // Formatters for time display
        val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dateFormatterYear = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return when {
            calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
                // Not in the current year
                dateFormatterYear.format(calendar.time)
            }

            calendar.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) -> {
                // Earlier this year
                dateFormatter.format(calendar.time)
            }

            else -> {
                // Today
                timeFormatter.format(calendar.time)
            }
        }
    }
}