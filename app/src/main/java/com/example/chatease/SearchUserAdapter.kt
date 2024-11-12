package com.example.chatease

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.databinding.SearchContentBinding
import com.example.chatease.databinding.SearchContentNotFoundBinding
import com.squareup.picasso.Picasso

// Adapter for displaying search results in a RecyclerView
class SearchUserAdapter(
    private val userData: MutableList<SearchUserData> // List holding search results
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
            val view = SearchContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UserProfileViewHolder(view)
        } else {
            // Inflate layout for "No Match Found" message if no results were found
            val view = SearchContentNotFoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UserNotFoundHolder(view)
        }
    }

    // ViewHolder for user profile items
    class UserProfileViewHolder(
        val binding: SearchContentBinding
    ) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder for "No Match Found" Layout
    class UserNotFoundHolder(
        private val binding: SearchContentNotFoundBinding
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
            holder.binding.textViewUserName.text = userData[position].userName
            Picasso.get().load(userData[position].userAvatar).into(holder.binding.roundedImageView) // Load profile image
        } else {
            // No binding needed for UserNotFoundHolder as "No Match Found" Layout will be shown
        }
    }
}
