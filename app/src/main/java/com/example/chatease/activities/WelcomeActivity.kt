package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivityWelcomePageBinding

import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding : ActivityWelcomePageBinding
    private val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityWelcomePageBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (auth.currentUser != null ) {
            startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
            finish()
        }
        else{
            binding.startMessagingButton.setOnClickListener {
                val intent = Intent(this@WelcomeActivity,SignInActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }


        }
    }
}