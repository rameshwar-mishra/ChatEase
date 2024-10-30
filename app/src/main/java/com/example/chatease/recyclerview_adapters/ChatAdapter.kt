package com.example.chatease.recyclerview_adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.dataclass.MessageUserData
import com.example.chatease.databinding.ReceiverMessageLayoutBinding
import com.example.chatease.databinding.SenderMessageLayoutBinding

class ChatAdapter(
    val messageList: MutableList<MessageUserData>,
    val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class SenderViewHolder(val binding: SenderMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}


    class ReceiverViewHolder(val binding: ReceiverMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    companion object {
        val IS_SENDER = 1
        val IS_RECEIVER = 2
    }

    override fun getItemViewType(position: Int): Int {
        if(messageList[position].sender == currentUserId) {
            return IS_SENDER
        } else {
            return IS_RECEIVER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == IS_SENDER) {
            val view = SenderMessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SenderViewHolder(view)
        } else {
            val view = ReceiverMessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ReceiverViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        if (messageList.size > 0) {
            return messageList.size
        } else return 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is SenderViewHolder) {
            holder.binding.textViewSenderMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].timestamp
        } else if (holder is ReceiverViewHolder){
            holder.binding.textViewReceiverMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].timestamp
        }
    }

}