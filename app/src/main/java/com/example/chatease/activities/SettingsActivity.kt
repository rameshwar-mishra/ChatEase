package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chatease.databinding.ActivitySettingsBinding
import com.example.chatease.listview_adapters.SettingsAdapter

class SettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.activityToolbar // Setting up the toolbar
        setSupportActionBar(toolbar) // Setting the toolbar as the app bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enabling the back button
        supportActionBar?.title = "Settings" // Setting title for the toolbar

        val itemList = listOf("Account", "Chats", "Privacy")

        binding.listView.adapter = SettingsAdapter(this@SettingsActivity, itemList)

        binding.listView.setOnItemClickListener { parent, view, position, id ->

            when (position) {
                0 -> {
                    startActivity(Intent(this@SettingsActivity,Settings_AccountActivity::class.java))
                }

                1 -> {
                    startActivity(Intent(this@SettingsActivity,Settings_ChatActivity::class.java))
                }

                2 -> {
                    startActivity(Intent(this@SettingsActivity,Settings_PrivacyActivity::class.java))
                }
            }
        }
    }
}