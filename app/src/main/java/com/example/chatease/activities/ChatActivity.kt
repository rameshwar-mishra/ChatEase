package com.example.chatease.activities

import android.os.Bundle
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {
    private lateinit var binding : ActivityChatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //Taking Custom Toolbar from view binding
        val toolbar = binding.chatActivityToolbar
        // adding custom toolbar to actual view
        setSupportActionBar(toolbar)
        //enables back button on toolbar to get back to parent activity defined under Manifest File
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //to disable the default app name on ActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean { // adding drop down menu option to the chat layout
        menuInflater.inflate(R.menu.menu_chat_options,menu)
        return true
    }
}

