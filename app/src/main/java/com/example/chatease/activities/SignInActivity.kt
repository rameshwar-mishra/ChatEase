package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class SignInActivity : AppCompatActivity() {
    lateinit var binding : ActivitySignInBinding
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //To be able Check if the user is coming from the SignUpActivity
        val fromSignUp = intent.getBooleanExtra("fromSignUp",false)

        //Checking if the user is already logged in AND is not coming from the SignUpActivity
        //Open the MainActivity and close the SignInActivity
        if(auth.currentUser != null && !fromSignUp) {
            startActivity(Intent(this@SignInActivity, MainActivity::class.java))
            finish()
        }

        //SignIn Button
        binding.buttonSignIn.setOnClickListener{
            isLoading(true)
            if(!isValidSignUp()) {
                return@setOnClickListener
            }
            signIn()
        }
        //starting Forget Password Activity
        binding.textViewForgetPassword.setOnClickListener {
            startActivity(Intent(this@SignInActivity,ForgetPasswordActivity::class.java))
        }
        //A textView of "Don't have an Account? Sign Up" using as a Button
        binding.textViewSignUp.setOnClickListener {
            Log.d("test","test")
            val intent = Intent(this@SignInActivity, SignUpActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this@SignInActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidSignUp(): Boolean {
        if (binding.editTextEmail.text.isNullOrEmpty()) {
            //if Email editText is empty
            isLoading(false)
            binding.editLayoutEmail.error = "Please fill the email field"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.editTextEmail.text.toString()).matches()) {
            //if Email is not valid
            isLoading(false)
            binding.editLayoutEmail.error = "Not a valid email"
            return false
        } else if (binding.editTextPassword.text.isNullOrEmpty()) {
            //if password editText is empty
            isLoading(false)
            binding.editLayoutEmail.error = null
            binding.editLayoutPassword.error = "Please fill the password field"
            return false
        } else if (binding.editTextPassword.text.toString().length < 6) {
            //if password is less than 6 letters, forced by Firebase Authentication Service
            isLoading(false)
            binding.editLayoutEmail.error = null
            binding.editLayoutPassword.error = "Password should contain atleast 6 letters"
        }
        return true
    }

    private fun isLoading(bool: Boolean) {
        //This is a Rounded progressBar
        if(bool) {
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonSignIn.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignIn.visibility = View.VISIBLE
        }
    }

    private fun signIn() {
        //Trying to Authenticate the user by creating the id
        auth.signInWithEmailAndPassword(binding.editTextEmail.text.toString().trim(), binding.editTextPassword.text.toString().trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isLoading(false)
                    startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                    finish()
                } else {
                    isLoading(false)
                    if(task.exception is FirebaseAuthException){
                        when((task.exception as FirebaseAuthException).errorCode){
                            "ERROR_USER_NOT_FOUND" -> {
                                binding.editLayoutEmail.error = "Invalid Sign In"
                                binding.editLayoutPassword.error = "Invalid Sign In"
                            }
                            "ERROR_INVALID_CREDENTIAL" -> {
                                binding.editLayoutPassword.error = "Invalid Credentials"
                            }
                            "ERROR_NETWORK_REQUEST_FAILED" ->{
                                binding.editLayoutEmail.error = "No Internet Connection"
                                binding.editLayoutPassword.error = "No Internet Connection"
                            }
                            "ERROR_TOO_MANY_REQUESTS" ->{
                                binding.editLayoutEmail.error = "Slow Down"
                                binding.editLayoutPassword.error = "Slow Down"
                            }
                        }
                    }

                }
            }
    }
}