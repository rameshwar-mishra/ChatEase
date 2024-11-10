package com.example.chatease.recyclerview_adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.databinding.ReceiverMessageLayoutBinding
import com.example.chatease.databinding.SenderMessageLayoutBinding
import com.example.chatease.databinding.UnreadMessageLayoutBinding
import com.example.chatease.dataclass.MessageUserData

// Adapter for managing chat messages in the RecyclerView
class ChatAdapter(
    private val messageList: MutableList<MessageUserData>, // List of messages to display
    private val currentUserId: String // ID of the current user
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ViewHolder for messages sent by the user
    class SenderViewHolder(val binding: SenderMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    // ViewHolder for messages received from other users
    class ReceiverViewHolder(val binding: ReceiverMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    class UnreadViewHolder(val binding: UnreadMessageLayoutBinding) : RecyclerView.ViewHolder(binding.root) {}

    companion object {
        const val IS_SENDER = 1 // Constant to represent sender view type
        const val IS_RECEIVER = 2 // Constant to represent receiver view type
        const val IS_UNREAD = 3 // Constant to represent unread message view type
    }

    // Determines the view type (sender or receiver) for a message
    override fun getItemViewType(position: Int): Int {
        return if (!messageList[position].hasRead && messageList[position].sender == "") {
            IS_UNREAD
        } else if (messageList[position].sender == currentUserId) {
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
        } else if (viewType == IS_RECEIVER) {
            // Inflate the layout for received messages
            val view = ReceiverMessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceiverViewHolder(view)
        } else {
            // Inflate the layout for unread message indicator/separator
            val view = UnreadMessageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UnreadViewHolder(view)
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
            if (!messageList[position].hasRead) {
                holder.binding.readIndicatorSingleTick.visibility = View.VISIBLE
                holder.binding.readIndicatorDoubleTick.visibility = View.GONE
            } else if (messageList[position].hasRead) {
                holder.binding.readIndicatorSingleTick.visibility = View.GONE
                holder.binding.readIndicatorDoubleTick.visibility = View.VISIBLE
            }
            holder.binding.textViewSenderMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].timestamp
        } else if (holder is ReceiverViewHolder) {
            // Bind data for receiver messages
            holder.binding.textViewReceiverMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].timestamp
        }
    }
}
