package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.UserProfileActivity
import com.example.chatease.databinding.LayoutGroupParticipantBinding
import com.example.chatease.dataclass.GroupProfileParticipantsData

class GroupParticipantsAdapter(
    private val context: Context,
    private val participantList: MutableList<GroupProfileParticipantsData>
) : RecyclerView.Adapter<GroupParticipantsAdapter.ViewHolder>() {

    class ViewHolder(val binding: LayoutGroupParticipantBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutGroupParticipantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return participantList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            if (participantList[position].displayName.length > 20) {
                val subStrDisplayName =
                    participantList[position].displayName.substring(0, 20) + "..."
                textViewDisplayName.text = subStrDisplayName
            } else {
                textViewDisplayName.text = participantList[position].displayName
            }

            textViewUserName.text = "@${participantList[position].userName}"
            if (participantList[position].role != "member") {
                textViewHierarchy.text = "Owner"
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
        }
    }
}

//            EC28CZ-WZN7M0