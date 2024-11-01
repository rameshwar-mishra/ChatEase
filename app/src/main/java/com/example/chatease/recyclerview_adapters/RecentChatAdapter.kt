package com.example.chatease.recyclerview_adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.databinding.RecentChatUserLayoutBinding
import com.example.chatease.dataclass.RecentChatData
import com.squareup.picasso.Picasso

class RecentChatAdapter(
    val context: Context,
    private val recentChatDataList : MutableList<RecentChatData>
) : RecyclerView.Adapter<RecentChatAdapter.RecentChatViewHolder>() {

    class RecentChatViewHolder(val binding : RecentChatUserLayoutBinding) : RecyclerView.ViewHolder(binding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentChatViewHolder {
        val view = RecentChatUserLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return RecentChatViewHolder(view)
    }

    override fun getItemCount(): Int {
        if(recentChatDataList.isNotEmpty()) {
            return recentChatDataList.size
        } else
            return 0
    }

    override fun onBindViewHolder(holder: RecentChatViewHolder, position: Int) {
        holder.binding.userName.text = recentChatDataList[position].displayName
        Picasso.get().load(recentChatDataList[position].avatar).into(holder.binding.userAvatar)
        holder.binding.userLastMessageTimeStamp.text = recentChatDataList[position].lastMessageTimeStamp
        holder.binding.userLastMessage.text = recentChatDataList[position].lastMessage
    }


}
