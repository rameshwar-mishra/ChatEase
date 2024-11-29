package com.example.chatease.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.chatease.R
import com.example.chatease.activities.Settings_AccountActivity
import com.example.chatease.activities.Settings_ChatActivity
import com.example.chatease.activities.Settings_PrivacyActivity
import com.example.chatease.adapters_listview.SettingsAdapter
import com.example.chatease.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = binding.activityToolbar // Setting up the toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar) // Setting the toolbar as the app bar
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)// Setting title for the toolbar

        val itemList = listOf(
            "Account",
            "Chats",
            "Privacy & Security")
        val itemListMetaDataText = listOf(
            "Change Account Information",
            "Manage Chat Wallpaper",
            "Change Privacy Settings for Users"
        )
        val itemListIcons = listOf(
            R.drawable.vector_icon_account_settings,
            R.drawable.vector_icon_chat,
            R.drawable.vector_icon_privacy
        )
        binding.listView.adapter = SettingsAdapter(requireContext(), itemList,itemListMetaDataText,itemListIcons)

        binding.listView.setOnItemClickListener { parent, view, position, id ->

            when (position) {
                0 -> {
                    startActivity(Intent(requireContext(), Settings_AccountActivity::class.java))
                }

                1 -> {
                    startActivity(Intent(requireContext(), Settings_ChatActivity::class.java))
                }

                2 -> {
                    startActivity(Intent(requireContext(), Settings_PrivacyActivity::class.java))
                }
            }
        }
    }
}