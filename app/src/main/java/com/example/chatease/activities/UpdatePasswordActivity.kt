package com.example.chatease.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivityUpdatePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class   UpdatePasswordActivity : AppCompatActivity() {
    val auth = FirebaseAuth.getInstance()
    lateinit var binding: ActivityUpdatePasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = binding.updatePasswordActivityToolbar
        setSupportActionBar(toolbar) // Set custom toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button on toolbar
        supportActionBar?.title = ""
        val user = FirebaseAuth.getInstance().currentUser
        binding.currentPassword.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                if (binding.currentPassword.text.toString().isEmpty()) {
                    binding.currentPasswordLayout.error = "Cannot Be Empty"
                } else if (binding.currentPassword.text.toString().length < 6) {
                    binding.currentPasswordLayout.error = "Password Must be more than 6 Characters"
                } else {
                    binding.currentPasswordLayout.error = null
                }
            }
        }
        binding.newPassword.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                if (binding.newPassword.text.toString().isEmpty()) {
                    binding.newPasswordLayout.error = "Cannot Be Empty"
                } else if (binding.newPassword.text.toString().length < 6) {
                    binding.newPasswordLayout.error = "Password Must be more than 6 Characters"
                } else {
                    binding.newPasswordLayout.error = null
                }
            }
        }
        binding.submitButton.setOnClickListener {
            binding.submitButton.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE

            if (binding.currentPassword.text.toString().isNotEmpty()
                && binding.newPassword.text.toString().isNotEmpty()
            ) {
                if (binding.currentPassword.text.toString().length >= 6
                    && binding.newPassword.text.toString().length >= 6
                ) {
//Getting User Email from EmailAuthProvider and current Password By User
                    val userDetails = EmailAuthProvider.getCredential(
                        user?.email!!,
                        binding.currentPassword.text.toString()
                    )

                    user?.reauthenticate(userDetails)?.addOnCompleteListener { authTask ->

                        if (authTask.isSuccessful) {
                            if (binding.currentPassword.text.toString() == binding.newPassword.text.toString()) {
                                binding.currentPasswordLayout.error = "Cannot Be Same"
                                binding.newPasswordLayout.error = "Cannot Be Same"
                                binding.progressBar.visibility = View.GONE
                                binding.submitButton.visibility = View.VISIBLE
                            } else {
                                user.updatePassword(binding.newPassword.text.toString())
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(
                                                this,
                                                "Password Successfully Changed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            binding.currentPassword.error = null
                                            binding.newPasswordLayout.error = null
                                            binding.progressBar.visibility = View.GONE
                                            binding.submitButton.visibility = View.VISIBLE
                                        }
                                    }
                            }

                        } else {
                            binding.currentPassword.error = null
                            binding.newPasswordLayout.error = null
                            Toast.makeText(this, "Failed to Update Password", Toast.LENGTH_SHORT)
                                .show()
                            binding.progressBar.visibility = View.GONE
                            binding.submitButton.visibility = View.VISIBLE
                        }
                    }
                } else {
                    if (binding.currentPassword.text.toString().length <= 5 && binding.newPassword.text.toString().length <= 5) {
                        binding.currentPasswordLayout.error =
                            "Password should contain atleast 6 letters"
                        binding.newPasswordLayout.error =
                            "Password should contain atleast 6 letters"
                    }
                    if (binding.newPassword.text.toString().length <= 5 && binding.currentPassword.text.toString().length > 5) {

                        binding.newPasswordLayout.error =
                            "Password should contain atleast 6 letters"
                    } else {
                        binding.currentPasswordLayout.error =
                            "Password should contain atleast 6 letters"
                    }

                    binding.progressBar.visibility = View.GONE
                    binding.submitButton.visibility = View.VISIBLE
                }
            } else {
                if (binding.currentPassword.text.toString()
                        .isEmpty() && binding.newPassword.text.toString().isEmpty()
                ) {
                    binding.currentPasswordLayout.error = "Cannot Be Empty"
                    binding.newPasswordLayout.error = "Cannot Be Empty"
                } else if (binding.newPassword.text.toString()
                        .isEmpty() && binding.currentPassword.text.toString().isNotEmpty()
                ) {
                    binding.newPasswordLayout.error = "Cannot Be Empty"
                } else {
                    binding.currentPasswordLayout.error = "Cannot Be Empty"
                }
                binding.progressBar.visibility = View.GONE
                binding.submitButton.visibility = View.VISIBLE


            }
        }
    }
}


