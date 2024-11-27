package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.UserProfileActivity
import com.example.chatease.databinding.LayoutRequestsBinding
import com.example.chatease.dataclass.SearchUserData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FrndRequestResponseAdapter(
    private val context: Context,
    private val userDataList: MutableList<SearchUserData>,
    private val usage: String
) : RecyclerView.Adapter<FrndRequestResponseAdapter.UserDataViewHolder>() {
    val rtDB = FirebaseDatabase.getInstance()

    class UserDataViewHolder(val binding: LayoutRequestsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserDataViewHolder {
        val view = LayoutRequestsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserDataViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userDataList.size
    }

    override fun onBindViewHolder(holder: UserDataViewHolder, position: Int) {
        holder.binding.apply {
            displayName.text = userDataList[position].displayName
            userName.text = userDataList[position].userName

            Glide.with(context)
                .load(userDataList[position].userAvatar)
                .placeholder(R.drawable.vector_default_user_avatar)
                .into(avatar)

            root.setOnClickListener {
                val intent = Intent(context, UserProfileActivity::class.java)
                intent.putExtra("id", userDataList[position].userID)
                context.startActivity(intent)
            }
        }

        if (usage == "Received") {
            holder.binding.frameLayoutAcceptButton.setOnClickListener {
                FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val map = async {
                            removeRequestsFromSentAndReceived(
                                currentUserID = currentUser.uid,
                                position = position,
                                usage = usage
                            )
                        }.await()

                        if (map["isSuccessful"] == "true") {
                            addIDInRequestAccepted(
                                currentUserID = currentUser.uid,
                                position = position,
                                otherUserId = map["otherUserId"]!!,
                                displayName = map["displayName"]!!
                            )
                        }
                    }
                }
            }

            holder.binding.frameLayoutDeclineButton.setOnClickListener {
                MaterialAlertDialogBuilder(context).apply {
                    setTitle("Decline Request")
                    setMessage("Are you sure, you want to decline the friend request?")
                    setIcon(R.drawable.vector_icon_warning)
                    setPositiveButton("Yes") { dialog, _ ->
                        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                            CoroutineScope(Dispatchers.IO).launch {
                                removeRequestsFromSentAndReceived(
                                    currentUserID = currentUser.uid,
                                    position = position,
                                    usage = usage
                                )
                            }
                        }
                        dialog.cancel()
                    }
                    setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }
                }.show()
            }
        } else {

            // usage == "sent"

            holder.binding.frameLayoutAcceptButton.visibility = View.GONE

            holder.binding.frameLayoutDeclineButton.setOnClickListener {
                MaterialAlertDialogBuilder(context).apply {
                    setTitle("Decline Request")
                    setMessage("Are you sure, you want to withdraw the friend request?")
                    setIcon(R.drawable.vector_icon_warning)
                    setPositiveButton("Yes") { dialog, _ ->
                        FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                            CoroutineScope(Dispatchers.IO).launch {
                                removeRequestsFromSentAndReceived(
                                    currentUserID = currentUser.uid,
                                    position = position,
                                    usage = usage
                                )
                            }
                        }
                        dialog.cancel()
                    }
                    setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }
                }.show()
            }
        }
    }

    private suspend fun removeRequestsFromSentAndReceived(
        currentUserID: String,
        position: Int,
        usage: String
    ): Map<String, String> {
        return withContext(Dispatchers.IO) {
            val isSuccessful = async { removeFromOtherUserDB(currentUserID, position, usage) }.await()
            if (isSuccessful) {
                val otherUserId = userDataList[position].userID
                val displayName = userDataList[position].displayName
                if (usage == "Received") {
                    rtDB.getReference("users/$currentUserID/friends/requestReceived/${userDataList[position].userID}")
                        .removeValue()
                } else {
                    rtDB.getReference("users/$currentUserID/friends/requestSent/${userDataList[position].userID}")
                        .removeValue()
                }
                mapOf(
                    "isSuccessful" to "true",
                    "otherUserId" to otherUserId,
                    "displayName" to displayName
                )
            } else {
                mapOf(
                    "isSuccessful" to "true",
                )
            }
        }
    }

    private suspend fun removeFromOtherUserDB(currentUserID: String, position: Int, usage: String): Boolean {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { coroutine ->
                if (usage == "Received") {
                    rtDB.getReference("users/${userDataList[position].userID}/friends/requestSent/$currentUserID")
                        .removeValue()
                        .addOnSuccessListener {
                            coroutine.resume(true)
                        }
                        .addOnFailureListener { exception ->
                            coroutine.resumeWithException(exception)
                        }
                } else {
                    rtDB.getReference("users/${userDataList[position].userID}/friends/requestReceived/$currentUserID")
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

    private fun addIDInRequestAccepted(currentUserID: String, position: Int, otherUserId: String, displayName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val isSuccessful = async { addIdInOtherUserDB(currentUserID, position) }.await()
            if (isSuccessful) {
                rtDB.getReference("users/$currentUserID/friends/requestAccepted").updateChildren(
                    mapOf(
                        otherUserId to true
                    )
                ).addOnSuccessListener {
                    Toast.makeText(
                        context,
                        "You are now friends with $displayName",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun addIdInOtherUserDB(currentUserID: String, position: Int): Boolean {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { coroutine ->
                rtDB.getReference("users/${userDataList[position].userID}/friends/requestAccepted").updateChildren(
                    mapOf(
                        currentUserID to true
                    )
                )
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