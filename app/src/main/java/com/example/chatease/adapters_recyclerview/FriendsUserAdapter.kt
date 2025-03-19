package com.example.chatease.adapters_recyclerview

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.UserProfileActivity
import com.example.chatease.databinding.LayoutUserBinding
import com.example.chatease.dataclass.UserData

class FriendsUserAdapter(
    private val context: Context,
    private val userData: MutableList<UserData>, // List holding search results,
) : RecyclerView.Adapter<FriendsUserAdapter.UserProfileViewHolder>() {

    // ViewHolder for user profile items
    class UserProfileViewHolder(val binding: LayoutUserBinding) : RecyclerView.ViewHolder(binding.root)

    // Inflates the appropriate layout based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserProfileViewHolder {
        // Inflate layout for user profile if results were found
        val view = LayoutUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserProfileViewHolder(view)
    }

    // Returns the total number of items in the adapter
    override fun getItemCount(): Int {
        return userData.size
    }

    // Binds data to the view holder based on its type
    override fun onBindViewHolder(holder: UserProfileViewHolder, position: Int) {
        // If holder is UserProfileViewHolder, bind user data
        // Load profile image
        holder.binding.textViewUserName.text = "@${userData[position].userName}"
        holder.binding.textViewDisplayName.text = userData[position].displayName

        Glide.with(context)
            .load(userData[position].userAvatar)
            .placeholder(R.drawable.vector_default_user_avatar)
            .into(holder.binding.roundedImageView)

        holder.binding.searchUserLinearLayout.setOnClickListener {
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.apply {
                putExtra("id", userData[holder.adapterPosition].userID)
                putExtra("FromAnotherActivity", true)
            }
            context.startActivity(intent)
        }
    }
}