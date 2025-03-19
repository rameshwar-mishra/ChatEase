package com.example.chatease.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.adapters_fragment.FragmentAdapter
import com.example.chatease.databinding.ActivityFriendRequestsBinding
import com.example.chatease.fragments.RequestReceivedFragment
import com.example.chatease.fragments.RequestSentFragment
import com.google.android.material.tabs.TabLayoutMediator

class FriendRequestsActivity : AppCompatActivity() {
    private lateinit var binding : ActivityFriendRequestsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.toolbar // Get reference to toolbar
        setSupportActionBar(toolbar) // Set toolbar as the action bar for the activity
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button in toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false) // Disable the title to be visible in the toolbar

        binding.tabLayout.apply {
            addTab(newTab().setText("Received"))
            addTab(newTab().setText("Sent"))
        }

        val fragments = listOf(
            RequestReceivedFragment(),
            RequestSentFragment()
        )

        binding.viewPager2.adapter = FragmentAdapter(this, fragmentList = fragments)

        TabLayoutMediator(binding.tabLayout,binding.viewPager2) { tab, position ->
            tab.text = when(position) {
                0 -> "Received"
                1 -> "Sent"
                else -> null
            }
        }.attach()

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}