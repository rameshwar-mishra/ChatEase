package com.example.chatease.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivitySettingsPrivacyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Settings_PrivacyActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsPrivacyBinding
    private val auth = FirebaseAuth.getInstance()
    private val rtDB = FirebaseDatabase.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.activityToolbar // Setting up the toolbar
        setSupportActionBar(toolbar) // Setting the toolbar as the app bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enabling the back button
        supportActionBar?.setDisplayShowTitleEnabled(false)

        auth.currentUser?.let { currentUser ->
            rtDB.getReference("users/${currentUser.uid}").get()
                .addOnSuccessListener { task ->
                    val lastSeenAndOnlinePersonalSetting = task.child("lastSeenAndOnlineSetting").getValue(Boolean::class.java) ?: false
                    binding.materialSwitch.isChecked = lastSeenAndOnlinePersonalSetting
                }
        }


        binding.materialSwitch.setOnCheckedChangeListener { _, isChecked ->
            val currentValue =
                getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE).getBoolean("lastSeenAndOnlineSetting", false)
            if (isChecked) {
                if (!currentValue) {
                    auth.currentUser?.let { currentUser ->

                        rtDB.getReference("users/${currentUser.uid}")
                            .updateChildren(
                                mapOf(
                                    "lastSeenAndOnlineSetting" to true
                                )
                            )
                            .addOnSuccessListener {
                                getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("lastSeenAndOnlineSetting", true).apply()
                            }
                            .addOnFailureListener {
                                binding.materialSwitch.isChecked = false
                            }
                    }
                }

            } else {
                if (currentValue) {
                    auth.currentUser?.let { currentUser ->

                        rtDB.getReference("users/$currentUser")
                            .updateChildren(
                                mapOf(
                                    "lastSeenAndOnlineSetting" to false
                                )
                            )
                            .addOnSuccessListener {
                                getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("lastSeenAndOnlineSetting", false).apply()
                            }
                            .addOnFailureListener {
                                binding.materialSwitch.isChecked = true
                            }
                    }
                }
            }
        }
    }
}