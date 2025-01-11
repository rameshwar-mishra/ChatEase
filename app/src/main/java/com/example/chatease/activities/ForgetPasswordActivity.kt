package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivityForgetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    lateinit var binding: ActivityForgetPasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.forgetPasswordToolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.forgetPasswordButton.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.forgetPasswordButton.visibility = View.INVISIBLE
            if (binding.textInputEmail.text.toString().trim() != null) {
                if(Patterns.EMAIL_ADDRESS.matcher(binding.textInputEmail.text.toString()).matches()){
                    forgotPassword()
                }
                else{
                    binding.textInputLayoutEmail.error = "Enter Valid Email"
                }
            }
        }


    }

    private fun forgotPassword() {
        auth.sendPasswordResetEmail(binding.textInputEmail.text.toString().trim())
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    binding.progressBar.visibility = View.INVISIBLE
                    binding.forgetPasswordButton.visibility = View.VISIBLE
                    Toast.makeText(
                        this,
                        "Successfully Sent Password Reset Mail",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    binding.textInputLayoutEmail.error = "Enter Valid Email"
                }
            }
    }
}
