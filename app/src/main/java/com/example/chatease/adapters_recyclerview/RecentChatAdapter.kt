package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.ChatActivity
import com.example.chatease.databinding.LayoutRecentChatUserBinding
import com.example.chatease.dataclass.RecentChatData

class RecentChatAdapter(
    val context: Context, // Context for starting activities
    private val recentChatDataList: MutableList<RecentChatData> // List of recent chat data
) : RecyclerView.Adapter<RecentChatAdapter.RecentChatViewHolder>() {

    // ViewHolder class for binding recent chat layout
    class RecentChatViewHolder(val binding: LayoutRecentChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {}

    // Create new ViewHolder instances
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentChatViewHolder {
        val view =
            LayoutRecentChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentChatViewHolder(view)
    }

    // Get the total number of items in the list
    override fun getItemCount(): Int {
        return recentChatDataList.size
    }

    // Bind data to the ViewHolder

    override fun onBindViewHolder(holder: RecentChatViewHolder, position: Int) {
        holder.binding.apply {

            // Message Highlight setup based on Read Receipt
            if (!recentChatDataList[position].isLastMessageReadByMe) {
                // If the user hasn't seen the last message
                // HighLight it
                displayName.setTypeface(null, Typeface.BOLD)
                userLastMessage.setTypeface(null, Typeface.BOLD)
                userLastMessage.setTextColor(userLastMessage.context.getColor(R.color.textColors))
                userLastMessageTimeStamp.setTypeface(null, Typeface.BOLD)
            } else {
                // If the user has seen the last message
                // Remove the highlight
                displayName.setTypeface(null, Typeface.NORMAL)
                userLastMessage.setTypeface(null, Typeface.NORMAL)
                userLastMessage.setTextColor(Color.parseColor("#746C6C"))
                userLastMessageTimeStamp.setTypeface(null, Typeface.NORMAL)
            }
            // Set the last message timestamp
            userLastMessageTimeStamp.text = recentChatDataList[position].lastMessageTimeStamp

            //formatted string for display name if it is more than 18 chars
            displayName.text = subString(recentChatDataList[position].displayName, 18)

            // Create a formatted string for the last message
            val lastMessage =
                recentChatDataList[position].lastMessageSender + ": " + recentChatDataList[position].lastMessage
            // If the message is longer than 35 characters, truncate it
            userLastMessage.text = subString(lastMessage, 35)

            Glide.with(holder.binding.userAvatar.context)
                .load(recentChatDataList[position].avatar)
                .placeholder(R.drawable.vector_default_user_avatar)
                .into(holder.binding.userAvatar)
            if(!recentChatDataList[position].avatar.isNullOrEmpty()){
                holder.binding.userAvatar.setPadding(0,0,0,0)
            }
            else{
                holder.binding.userAvatar.setPadding(20,20,20,20)
            }

            root.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java)
                intent.apply {
                    putExtra("id", recentChatDataList[position].id)
                }
                context.startActivity(intent) // Start the chat activity
            }

        }
    }

    private fun subString(subStringText: String, untilWhichLength: Int): String {
        if (subStringText.length > untilWhichLength) {
            val subStr = subStringText.substring(0, untilWhichLength)
            return "$subStr..."
        } else {
            return subStringText
        }
    }
}
