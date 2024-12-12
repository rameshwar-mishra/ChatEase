package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.GroupParticipantsActivity
import com.example.chatease.databinding.LayoutSearchContentNotFoundBinding
import com.example.chatease.databinding.LayoutSelectGrpParticipantsBinding
import com.example.chatease.dataclass.UserData
import com.google.firebase.auth.FirebaseAuth

class SelectGroupParticipantsAdapter(
    private val context: Context,
    private val userDataList: MutableList<UserData>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedParticipants = mutableSetOf<String>()

    class Layout(val binding: LayoutSelectGrpParticipantsBinding) : RecyclerView.ViewHolder(binding.root)

    class Layout2(val binding: LayoutSearchContentNotFoundBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        if (userDataList.isNotEmpty()) {
            return 1
        } else {
            return 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {
            1 -> {
                val view = LayoutSelectGrpParticipantsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return Layout(view)
            }

            else -> {
                val view = LayoutSearchContentNotFoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return Layout2(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return userDataList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        selectedParticipants.add(FirebaseAuth.getInstance().currentUser!!.uid)

        if (holder is Layout) {
            holder.binding.apply {
                textViewDisplayName.text = userDataList[position].displayName
                textViewUserName.text = "@${userDataList[position].userName}"

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
        } else if (holder is Layout2) {
            holder.binding.textViewContentNotFound.text = "You need to have friends to create a group"

            // Get the current LayoutParams of the RecyclerView
            val params = (context as GroupParticipantsActivity).binding.recyclerView.layoutParams

            // Ensure the LayoutParams are of the correct type
            if (params is ConstraintLayout.LayoutParams) {
                // Set height to 0dp
                params.height = 0

                // Set the bottomToBottomOf constraint to the parent
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                // Apply the updated LayoutParams to the RecyclerView
                (context as GroupParticipantsActivity).binding.recyclerView.layoutParams = params
            }
        }

    }

    fun getSelectedParticipantsSet(): MutableSet<String> {
        return selectedParticipants
    }
}