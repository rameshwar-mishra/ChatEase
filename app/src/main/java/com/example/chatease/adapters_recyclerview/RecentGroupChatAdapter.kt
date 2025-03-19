package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.GroupChatActivity
import com.example.chatease.databinding.LayoutRecentGroupChatBinding
import com.example.chatease.dataclass.RecentGroupChatData

class RecentGroupChatAdapter(
    private val context: Context,
    private val groupList: MutableList<RecentGroupChatData>
) : RecyclerView.Adapter<RecentGroupChatAdapter.ViewHolder>() {

    class ViewHolder(val binding: LayoutRecentGroupChatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutRecentGroupChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return groupList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            groupName.text = groupList[position].groupName

            if (!groupList[position].isLastMessageReadByMe) {
                // If the user hasn't seen the last message
                // HighLight it
                groupName.setTypeface(null, Typeface.BOLD)
                groupLastMessage.setTypeface(null, Typeface.BOLD)
                groupLastMessage.setTextColor(groupLastMessage.context.getColor(R.color.white))
                groupLastMessageTimeStamp.setTypeface(null, Typeface.BOLD)
            } else {
                // If the user has seen the last message
                // Remove the highlight
                groupName.setTypeface(null, Typeface.NORMAL)
                groupLastMessage.setTypeface(null, Typeface.NORMAL)
                groupLastMessage.setTextColor(Color.parseColor("#746C6C"))
                groupLastMessageTimeStamp.setTypeface(null, Typeface.NORMAL)
            }

            if(groupList[position].lastMessage.isNotEmpty()) {
                var lastMessage = "${groupList[position].lastMessageSender}: ${groupList[position].lastMessage}"

                if (lastMessage.length > 30) {
                    lastMessage.substring(0, 30)
                    lastMessage = "${lastMessage}...."
                }
                groupLastMessage.text = lastMessage
            } else {
                groupLastMessage.text = ""
            }

            Glide.with(context)
                .load(groupList[position].groupIcon)
                .placeholder(R.drawable.vector_icon_group)
                .into(groupIcon)

            if(groupList[position].groupIcon.isNotEmpty()) {
                val params = groupIcon.layoutParams
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                groupIcon.layoutParams = params
            } else {
                val params = groupIcon.layoutParams
                params.height = (30 * context.resources.displayMetrics.density).toInt()
                params.width = (30 * context.resources.displayMetrics.density).toInt()
                groupIcon.layoutParams = params
            }

            groupLastMessageTimeStamp.text = groupList[position].lastMessageTimeStamp

            root.setOnClickListener {
                val intent = Intent(context,GroupChatActivity::class.java)
                intent.putExtra("groupID",groupList[position].id)
                context.startActivity(intent)
            }
        }
    }
}