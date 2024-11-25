package com.example.chatease.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivitySettingsChatBinding

class Settings_ChatActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsChatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.activityToolbar // Setting up the toolbar
        setSupportActionBar(toolbar) // Setting the toolbar as the app bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enabling the back button
        supportActionBar?.setDisplayShowTitleEnabled(false)// Setting title for the toolbar

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }

            else -> return super.onOptionsItemSelected(item)

        }
        return true
    }
}