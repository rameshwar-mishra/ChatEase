package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.GroupProfileActivity
import com.example.chatease.activities.UserProfileActivity
import com.example.chatease.databinding.LayoutGroupParticipantBinding
import com.example.chatease.dataclass.GroupProfileParticipantsData

class GroupParticipantsAdapter(
    private val context: Context,
    private val participantList: MutableList<GroupProfileParticipantsData>
) : RecyclerView.Adapter<GroupParticipantsAdapter.ViewHolder>() {

    private var updateCounterValue = -1
    private var customInsertionInProgress = false
    private val insertionQueue = mutableListOf<Int>()

    class ViewHolder(val binding: LayoutGroupParticipantBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutGroupParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return participantList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            textViewDisplayName.text = participantList[position].displayName
            textViewUserName.text = "@${participantList[position].userName}"

            if (participantList[position].role != "member") {
                textViewHierarchy.text = participantList[position].role
                textViewHierarchy.visibility = View.VISIBLE
            }

            Glide.with(context)
                .load(participantList[position].userAvatar)
                .placeholder(R.drawable.vector_default_user_avatar)
                .into(roundedImageView)

            root.setOnClickListener {
                val intent = Intent(context, UserProfileActivity::class.java)
                intent.apply {
                    putExtra("id", participantList[position].userID)
                    putExtra("FromAnotherActivity", true)
                }
                context.startActivity(intent)
            }

            // ---initial----
            // 2
            // 0 : goku, 2 : TNA

            // ---------------
            // 0
            //
            Log.i("new", "onBindViewHolder customInsertionInProgress : $customInsertionInProgress")
            if (customInsertionInProgress) {
                updateCounterValue--
                Log.i("new", "onBindViewHolder updateCounterValue-- : $updateCounterValue")
                insertionQueue.removeAt(0)
                Log.i("new", "onBindViewHolder insertionQueue.removeAt : $insertionQueue")
                customInsertionInProgress = false
            }

            Log.i("new", "onBindViewHolder updateCounterValue > 0 :  ${updateCounterValue > 0}")
            if (updateCounterValue > 0) {
                (context as GroupProfileActivity).findViewById<RecyclerView>(R.id.recyclerViewParticipantsList).post{
                    Log.i("new", "onBindViewHolder notifyItemInserted : ${insertionQueue[0]}")
                    customInsertionInProgress = true
                    notifyItemInserted(insertionQueue[0])
                }
            }
        }
    }

    fun updateCounter(index: Int): Int {
        updateCounterValue++ // 2
        Log.w("new", "updateCounterValue : $updateCounterValue")
        Log.w("new", "updateCounterValue > 0 : ${updateCounterValue > 0}")
        if (updateCounterValue > 0) { // true
            insertionQueue.add(index)
            Log.w("new", "insertionQueue.add : $insertionQueue")
        }
        return updateCounterValue
    }
}

//            EC28CZ-WZN7M0