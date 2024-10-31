package com.example.chatease.recyclerview_adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.dataclass.MessageUserData
import com.example.chatease.databinding.ReceiverMessageLayoutBinding
import com.example.chatease.databinding.SenderMessageLayoutBinding

// Adapter for managing chat messages in the RecyclerView
class ChatAdapter(
    val messageList: MutableList<MessageUserData>, // List of messages to display
    val currentUserId: String // ID of the current user
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ViewHolder for messages sent by the user
    class SenderViewHolder(val binding: SenderMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    // ViewHolder for messages received from other users
    class ReceiverViewHolder(val binding: ReceiverMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    companion object {
        val IS_SENDER = 1 // Constant to represent sender view type
        val IS_RECEIVER = 2 // Constant to represent receiver view type
    }

    // Determines the view type (sender or receiver) for a message
    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].sender == currentUserId) {
            IS_SENDER // Message is sent by the current user
        } else {
            IS_RECEIVER // Message is received from another user
        }
    }

    // Creates the ViewHolder based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == IS_SENDER) {
            // Inflate the layout for sent messages
            val view = SenderMessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SenderViewHolder(view)
        } else {
            // Inflate the layout for received messages
            val view = ReceiverMessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceiverViewHolder(view)
        }
    }

    // Returns the total number of messages
    override fun getItemCount(): Int {
        return messageList.size // No need for additional checks, as the list size will be 0 if empty
    }

    // Binds the message data to the ViewHolder based on its type
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SenderViewHolder) {
            // Bind data for sender messages
            holder.binding.textViewSenderMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].timestamp
        } else if (holder is ReceiverViewHolder) {
            // Bind data for receiver messages
            holder.binding.textViewReceiverMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].timestamp
        }
    }
}
