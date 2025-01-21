package com.example.chatease.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.chatease.R
import com.example.chatease.adapters_fragment.FragmentAdapter
import com.example.chatease.databinding.ActivityMainBinding
import com.example.chatease.fragments.FriendsFragment
import com.example.chatease.fragments.GroupsFragment
import com.example.chatease.fragments.RecentChatFragment
import com.example.chatease.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var pageChangeCallback: OnPageChangeCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout and set it as the content view using view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set up window insets to ensure layout is not obscured by system bars (like status bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fragments = listOf(
            RecentChatFragment(),
            GroupsFragment(),
            FriendsFragment(),
            SettingsFragment()
        )

        // Setting up the adapter
        binding.viewpager2MainActivity.adapter = FragmentAdapter(this, fragmentList = fragments)

        // Handling Bottom Navigation clicks
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_group -> 1
                R.id.nav_friends -> 2
                R.id.nav_settings -> 3
                else -> 0
            }
            binding.viewpager2MainActivity.setCurrentItem(index, false)
            true
        }

        // Creating the callback
        pageChangeCallback = object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomNavigationView.menu[position].isChecked = true
            }
        }

        // registering the callback
        binding.viewpager2MainActivity.registerOnPageChangeCallback(pageChangeCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the callback to avoid memory leaks
        binding.viewpager2MainActivity.unregisterOnPageChangeCallback(pageChangeCallback)
    }
}