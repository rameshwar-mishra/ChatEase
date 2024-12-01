package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.databinding.LayoutSelectGrpParticipantsBinding
import com.example.chatease.dataclass.UserData

class SelectGroupParticipantsAdapter(
    private val context: Context,
    private val userDataList: MutableList<UserData>,
) : RecyclerView.Adapter<SelectGroupParticipantsAdapter.Layout>() {

    private val selectedParticipants = mutableSetOf<String>()

    class Layout(val binding: LayoutSelectGrpParticipantsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Layout {
        val view = LayoutSelectGrpParticipantsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Layout(view)
    }

    override fun getItemCount(): Int {
        return userDataList.size
    }

    override fun onBindViewHolder(holder: Layout, position: Int) {
        holder.binding.apply {
            textViewUserName.text = userDataList[position].userName
            textViewDisplayName.text = userDataList[position].displayName

            Glide.with(context)
                .load(userDataList[position].userAvatar)
                .placeholder(R.drawable.vector_default_user_avatar)
                .into(roundedImageViewAvatar)

            root.setOnClickListener {
                if (selectedParticipants.contains(userDataList[position].userID)) {
                    selectedParticipants.remove(userDataList[position].userID)
                    radioButton.isChecked = false
                } else {
                    selectedParticipants.add(userDataList[position].userID)
                    radioButton.isChecked = true
                }
            }
        }
    }

    fun getSelectedParticipantsSet(): MutableSet<String> {
        return selectedParticipants
    }
}