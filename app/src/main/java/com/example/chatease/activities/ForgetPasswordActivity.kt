package com.example.chatease.activities

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
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
        binding.forgetPasswordButton.setOnClickListener {
            if (binding.textInputEmail.text.toString().trim() != null) {
                if(Patterns.EMAIL_ADDRESS.matcher(binding.textInputEmail.text.toString()).matches()){
                    forgotPassword()
                }
                else{
                    binding.textInputLayoutEmail.error = "Enter Valid Email"
                }
<<<<<<< HEAD
            }
        }
=======

            }


        }

>>>>>>> 0745b7177c06f55aac6c8a9ab7f4ddce1fbeaeb3
    }

    private fun forgotPassword() {
        auth.sendPasswordResetEmail(binding.textInputEmail.text.toString().trim())
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

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

