package com.example.chatease

import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.databinding.SearchContentBinding
import com.example.chatease.databinding.SearchContentNotFoundBinding
import com.squareup.picasso.Picasso

class SearchUserAdapter(
    val userData:MutableList<SearchUserData>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object{
        const val isFound = 1
        const val isNotFound = 0
    }

    private var hasSearched = false

    fun updateSearchState(hasResults: Boolean) {
        hasSearched = true
        if(!hasResults) {
            userData.clear()
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if(userData.isEmpty() && hasSearched){
            return isNotFound
        }
        else
        return isFound
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if(viewType == isFound) {
            val view = SearchContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserProfileViewHolder(view)
        }
        else{
            val view = SearchContentNotFoundBinding.inflate(LayoutInflater.from(parent.context),parent,false)
            return UserNotFoundHolder(view)
        }



    }

    class UserProfileViewHolder(
        val binding:SearchContentBinding
    ) : RecyclerView.ViewHolder(binding.root){

    }
    class UserNotFoundHolder(
        val binding : SearchContentNotFoundBinding
    ):RecyclerView.ViewHolder(binding.root){

    }
    override fun getItemCount(): Int {
        if(userData.isEmpty() && hasSearched){
            return 1
        }
        else
        return userData.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is UserProfileViewHolder){
            holder.binding.textViewUserName.text = userData[position].userName
            Picasso.get().load(userData[position].userAvatar).into(holder.binding.roundedImageView)
        }
        else{

        }
    }

}