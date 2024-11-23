package com.example.chatease.adapters_fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.chatease.fragments.FriendsFragment
import com.example.chatease.fragments.GroupsFragment
import com.example.chatease.fragments.RecentChatFragment
import com.example.chatease.fragments.SettingsFragment

class FragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments = listOf(
        RecentChatFragment(),
        GroupsFragment(),
        FriendsFragment(),
        SettingsFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}