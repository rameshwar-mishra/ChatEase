package com.example.chatease.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class SignUpActivity : AppCompatActivity() {
    //View Binding
    private lateinit var binding: ActivitySignUpBinding
    //For requesting the image by opening android's file manager activity and then to be able to handle it later.
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    //For authenticating users
    private val auth = FirebaseAuth.getInstance()
    //For Storing user details
    private val db = Firebase.firestore
    //For Storing images
    private val storage = FirebaseStorage.getInstance().reference
    //For holding local image address
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //This needs to register in the onCreate event to be able to use it later on.
        registerActivityResultLauncher()

        //all the button event listeners
        setListeners()
    }

    private fun setListeners() {
        //Image Holder / Rounded ImageView
        binding.displayImage.setOnClickListener {
            chooseImage()
        }

        //SignUp Button
        binding.buttonSignUp.setOnClickListener {
            isLoading(true)
            if (!isValidSignUp()) {
                return@setOnClickListener
            } else {
                signUp()
            }

        }

        //A textView of "Already have an account? SignIn" using as a Button
        binding.textViewSignIn.setOnClickListener {
            val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun chooseImage() {
        // Checking permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Requesting permission
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        } else {
            // Permission granted, launch image picker from Android's File Manager
            val intent = Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
            }
            //To understand what happened after launching the image picker go to line number 139
            activityResultLauncher.launch(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //To be able to handle anything happens after requesting the permission from the user,
        // such as Allowing or Denying the permission

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Calling chooseImage again to launch the intent
                chooseImage()
            } else {
                //if the user denied then sending the user to the app's setting page to manually give the permission
                // as without the permission the feature is not going to work anyway
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                showToast("The app needs file permission to access images")
            }
        }
    }


    private fun registerActivityResultLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback { result ->
                //If we got an image
                if (result.resultCode == RESULT_OK && result.data != null)
                    imageUri = result.data?.data

                imageUri?.let {
                    //Using Picasso library to load the image to the Rounded ImageView using local image address/URI
                    Picasso.get().load(it).into(binding.displayImage)
                    //Setting the "Add Image" text to INVISIBLE as the image is now loaded
                    binding.displayImageText.visibility = View.INVISIBLE
                }
            })
    }

    private fun isValidSignUp(): Boolean {

        if (binding.editTextDisplayName.text.isNullOrEmpty()) {
            //if Username/Display name editText is empty
            isLoading(false)
            binding.editLayoutDisplayName.error = "Please fill the display name field"
            return false
        } else if (binding.editTextEmail.text.isNullOrEmpty()) {
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
        } else if (binding.editTextConfirmPassword.text.isNullOrEmpty()) {
            //if confirm password editText is empty
            isLoading(false)
            binding.editLayoutEmail.error = null
            binding.editLayoutPassword.error = null
            binding.editTextConfirmPassword.error = "Please fill the confirm password field"
            return false
        } else if (!binding.editTextPassword.text.toString().trim().equals(binding.editTextConfirmPassword.text.toString().trim())) {
            //if password and confirm password doesn't matches
            isLoading(false)
            binding.editLayoutEmail.error = null
            binding.editLayoutPassword.error = null
            binding.editTextConfirmPassword.error = "Password doesn't matches"
            return false
        } else if (imageUri == null) {
            //if Rounded ImageView is empty / If image is not added
            isLoading(false)
            binding.editLayoutEmail.error = null
            binding.editLayoutPassword.error = null
            binding.editTextConfirmPassword.error = null
            binding.displayImageText.setTextColor(getColor(R.color.red))
            return false
        }
        return true
    }

    private fun isLoading(bool: Boolean) {
        //This is a Rounded progressBar
        if (bool) {
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonSignUp.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignUp.visibility = View.VISIBLE
        }

    }

    private fun signUp() {
        //Trying to Authenticate the user by creating the id
        auth.createUserWithEmailAndPassword(
            binding.editTextEmail.text.toString().trim(),
            binding.editTextPassword.text.toString().trim()
        )
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    //Trying to store the Display Image / Profile Picture of the user in the Firebase Cloud Storage
                    val imageRef = storage.child("displayImage/${auth.currentUser!!.uid}")
                    imageRef.putFile(imageUri!!).addOnCompleteListener { uploadTask ->

                        if (uploadTask.isSuccessful) {
                            imageRef.downloadUrl.addOnCompleteListener { urlTask ->

                                if (urlTask.isSuccessful) {
                                    //Creating a hashmap of the userdata to store it in the Firebase Firestore
                                    val userDetails = hashMapOf(
                                        "username" to binding.editTextDisplayName.text.toString(),
                                        "email" to binding.editTextEmail.text.toString(),
                                        "displayImage" to urlTask.result.toString()
                                    )

                                    //Trying to store the userdata in the Firebase Firestore
                                    db.collection("users").document(auth.currentUser!!.uid).set(userDetails)
                                        .addOnCompleteListener { firestoreTask ->
                                            if (firestoreTask.isSuccessful) {
                                                isLoading(false)
                                                showToast("Signed Up Successfully")

                                                //Sending the user back to Sign In activity
                                                val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
                                                intent.putExtra("fromSignUp",true)
                                                startActivity(intent)
                                                finish()

                                            } else {
                                                isLoading(false)
                                                showToast("Failed to save user data")
                                                Log.d("SignUpError", firestoreTask.exception.toString())
                                            }
                                        }
                                } else {
                                    isLoading(false)
                                    showToast("Failed to get download URL")
                                    Log.d("SignUpError", urlTask.exception.toString())
                                }
                            }
                        } else {
                            isLoading(false)
                            showToast("Failed to upload image")
                            Log.d("SignUpError", uploadTask.exception.toString())
                        }
                    }
                } else {
                    isLoading(false)
                    showToast("Authentication failed")
                    Log.d("SignUpError", authTask.exception.toString())
                }
            }
    }
}
