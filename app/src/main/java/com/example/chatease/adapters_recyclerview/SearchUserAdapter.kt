package com.example.chatease.adapters_recyclerview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.activities.ChatActivity
import com.example.chatease.databinding.LayoutSearchContentNotFoundBinding
import com.example.chatease.databinding.LayoutUserBinding
import com.example.chatease.dataclass.UserData

// Adapter for displaying search results in a RecyclerView
class SearchUserAdapter(
    private val context: Context,
    private val userData: MutableList<UserData>, // List holding search results,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val isFound = 1 // View type for found search results
        const val isNotFound = 0 // View type for "not found" message
    }

    private var hasSearched = false // Flag to indicate if a search was performed

    // Custom Function to update search state based on whether results were found
    fun updateSearchState(hasResults: Boolean) {
        hasSearched = true
        if (!hasResults) {
            userData.clear() // Clear list if no results found
        }
        notifyDataSetChanged() // Notify RecyclerView to refresh the data
    }

    // Determines the view type for each item
    override fun getItemViewType(position: Int): Int {
        // If no results found and a search has been performed, show "No Match Found" Layout
        if (userData.isEmpty() && hasSearched) {
            return isNotFound
        } else {
            return isFound
        }
    }

    // Inflates the appropriate layout based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == isFound) {
            // Inflate layout for user profile if results were found
            val view = LayoutUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UserProfileViewHolder(view)
        } else {
            // Inflate layout for "No Match Found" message if no results were found
            val view = LayoutSearchContentNotFoundBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            UserNotFoundHolder(view)
        }
    }

    // ViewHolder for user profile items
    class UserProfileViewHolder(
        val binding: LayoutUserBinding
    ) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder for "No Match Found" Layout
    class UserNotFoundHolder(
        val binding: LayoutSearchContentNotFoundBinding
    ) : RecyclerView.ViewHolder(binding.root)

    // Returns the total number of items in the adapter
    override fun getItemCount(): Int {
        // If no results found and search performed, return 1 for "No Match Found" Layout
        if (userData.isEmpty() && hasSearched) {
            return 1
        } else {
            return userData.size
        }
    }

    // Binds data to the view holder based on its type
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is UserProfileViewHolder) {
            // If holder is UserProfileViewHolder, bind user data
            // Load profile image
            holder.binding.textViewUserName.text = "@${userData[position].userName}"
            if (userData[position].displayName.length > 20) {
                val subStr = userData[position].displayName.substring(0, 20) + "..."
                holder.binding.textViewDisplayName.text = subStr
            } else {
                holder.binding.textViewDisplayName.text = userData[position].displayName
            }


            Glide.with(context)
                .load(userData[position].userAvatar)
                .placeholder(R.drawable.vector_default_user_avatar)
                .into(holder.binding.roundedImageView)

            holder.binding.searchUserLinearLayout.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java)
                intent.apply {
                    putExtra("id", userData[position].userID)
                }
                context.startActivity(intent)
                if (context is Activity) {
                    context.finish()
                }
            }
        } else {
            // No binding needed for UserNotFoundHolder as "No Match Found" Layout will be shown
        }
    }
}