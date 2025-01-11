package com.example.chatease.adapters_recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.databinding.LayoutGroupReceiverMessageBinding
import com.example.chatease.databinding.LayoutSenderMessageBinding
import com.example.chatease.databinding.LayoutUnreadMessageBinding
import com.example.chatease.dataclass.GroupMessageData

class GroupChatAdapter(
    private val messageList: MutableList<GroupMessageData>, // List of messages to display
    private val currentUserId: String // ID of the current user
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // ViewHolder for messages sent by the user
    class SenderViewHolder(val binding: LayoutSenderMessageBinding) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder for messages received from other users
    class ReceiverViewHolder(val binding: LayoutGroupReceiverMessageBinding) : RecyclerView.ViewHolder(binding.root)

    class UnreadViewHolder(val binding: LayoutUnreadMessageBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        const val IS_SENDER = 1 // Constant to represent sender view type
        const val IS_RECEIVER = 2 // Constant to represent receiver view type
        const val IS_UNREAD = 3 // Constant to represent unread message view type
    }

    // Determines the view type (sender or receiver) for a message
    override fun getItemViewType(position: Int): Int {
        return if (!messageList[position].everyoneRead && messageList[position].senderID == "") {
            IS_UNREAD
        } else if (messageList[position].senderID == currentUserId) {
            IS_SENDER // Message is sent by the current user
        } else {
            IS_RECEIVER // Message is received from another user
        }
    }

    // Creates the ViewHolder based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            IS_SENDER -> {
                // Inflate the layout for sent messages
                val view = LayoutSenderMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SenderViewHolder(view)
            }

            IS_RECEIVER -> {
                // Inflate the layout for received messages
                val view = LayoutGroupReceiverMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceiverViewHolder(view)
            }

            else -> {
                // Inflate the layout for unread message indicator/separator
                val view = LayoutUnreadMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                UnreadViewHolder(view)
            }
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
//            Log.d("runs tick check", messageList[position].content + " " + messageList[position].everyoneRead.toString())
            if (!messageList[position].everyoneRead) {
                holder.binding.readIndicatorSingleTick.visibility = View.VISIBLE
                holder.binding.readIndicatorDoubleTick.visibility = View.GONE
            } else if (messageList[position].everyoneRead) {
                holder.binding.readIndicatorSingleTick.visibility = View.GONE
                holder.binding.readIndicatorDoubleTick.visibility = View.VISIBLE
            }
            holder.binding.textViewSenderMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].formattedTimestamp
        } else if (holder is ReceiverViewHolder) {
            // Bind data for receiver messages
            //Checking to ensure that Timestamp is getting correctly constraint to right Textview Based on the length
            if(messageList[position].senderName.length > messageList[position].content.length){
                val constraintLayoutReceiver = ConstraintSet()
                constraintLayoutReceiver.clone(holder.binding.constraintLayoutGroupReceiver)
                constraintLayoutReceiver.clear(holder.binding.textViewTimeStamp.id,ConstraintSet.START)
                constraintLayoutReceiver.connect(
                    holder.binding.textViewTimeStamp.id,
                    ConstraintSet.START,
                    holder.binding.textViewSenderName.id,
                    ConstraintSet.END,
                    2
                    )
                Log.e("checking","came to if block")
                constraintLayoutReceiver.applyTo(holder.binding.constraintLayoutGroupReceiver)
            }
            holder.binding.textViewSenderName.text = messageList[position].senderName
            holder.binding.textViewReceiverMessage.text = messageList[position].content
            holder.binding.textViewTimeStamp.text = messageList[position].formattedTimestamp
        }
    }

}