package com.example.chatease

import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.databinding.SearchContentBinding
import com.example.chatease.databinding.SearchContentNotFoundBinding
import com.squareup.picasso.Picasso

class SearchUserAdapter(
    private val userData: MutableList<SearchUserData>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val isFound = 1
        const val isNotFound = 0
    }

    // A variable to check if the Search happened
    private var hasSearched = false

    fun updateSearchState(hasResults: Boolean) {
        // If this function invokes that means the user has searched something
        hasSearched = true

        // Checks if the user's query matched something in the database
        if (!hasResults) {
            // if the query doesnt matched to the database & result not found
            // clears any data the list has
            userData.clear()
        }
        // Notifies the adapter to update the data
        notifyDataSetChanged()
    }

    // This Event listener gets invoked before onCreateViewHolder
    // which can be useful to decide which layout should be used
    // before we actually create the view holder using that layout

    override fun getItemViewType(position: Int): Int {
        // Checks if the MutableList of UserObject is empty && The Search happened
        if (userData.isEmpty() && hasSearched) {
            // returns isNotFound, which will be used in the onCreateViewHolder to detect which layout should be used
            return isNotFound
        } else
        // returns isFound, which will be used in the onCreateViewHolder to detect which layout should be used
            return isFound
    }

    //This Event listener gets invoked after getItemViewType
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Checks if something is found via isFound variable which we are using here as a flag/indicator
        if (viewType == isFound) {
            // Inflating the search Result layout to show the results
            // using the ViewBinding to inflate
            val view = SearchContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserProfileViewHolder(view)
        } else {
            // Inflating the No Match Found layout to show the that Nothing matched the query
            // using the ViewBinding to inflate
            val view = SearchContentNotFoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserNotFoundHolder(view)
        }


    }

    // This is the view holder class of the Search Result Layout
    // We're using ViewBinding
    class UserProfileViewHolder(
        val binding: SearchContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {}

    // This is the view holder class of the No Match Found Layout
    // We're using ViewBinding
    class UserNotFoundHolder(
        val binding: SearchContentNotFoundBinding
    ) : RecyclerView.ViewHolder(binding.root) {}

    // This event listener is used to determine how many copies of the layout should be shown
    override fun getItemCount(): Int {
        if (userData.isEmpty() && hasSearched) {
            return 1
        } else
            return userData.size
    }

    // This event listener is used to bind the data to the components ( Think of it as pass/supplying the data to the respective components)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Checks if the holder is from UserProfileViewHolder Class
        if (holder is UserProfileViewHolder) {
            // Populates the respective components with their respective data
            holder.binding.textViewUserName.text = userData[position].userName
            Picasso.get().load(userData[position].userAvatar).into(holder.binding.roundedImageView)
        } else {
            // Does nothing as No Match Found
        }
    }

}