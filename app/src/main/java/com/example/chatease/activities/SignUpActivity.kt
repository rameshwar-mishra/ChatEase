package com.example.chatease.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File

class SignUpActivity : AppCompatActivity() {
    // View Binding for accessing the views in the layout
    private lateinit var binding: ActivitySignUpBinding

    // Activity result launcher for handling image picking result
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // Firebase Auth instance for authenticating users
    private val auth = FirebaseAuth.getInstance()

    // Firestore instance for storing user details
    private val db = Firebase.firestore

    // Firebase RealTime Database instance for storing user details
    private val rtDB = FirebaseDatabase.getInstance()

    // Firebase Storage reference for storing images
    private val storage = FirebaseStorage.getInstance().reference

    // URI for holding the local image address
    private var imageUri: Uri? = null

    // Bitmap object for the selected image
    private lateinit var bitmap: Bitmap

    // ByteArray for storing the compressed image
    private var compressedImageAsByteArray: ByteArray? = null

    // Destination URI for the cropped image
    private lateinit var destinationUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the view using view binding
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setting up the URI for the cropped image
        destinationUri = Uri.fromFile(File(cacheDir, "temp_cropped_image.webp"))

        // Adjusting the layout for system window insets (like status bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Register the activity result launcher to handle image picking
        registerActivityResultLauncher()
        binding.editTextUserName.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                if (binding.editTextUserName.text!!.isNotEmpty()) {
                    if (!binding.editTextUserName.text.toString().matches(Regex("^[a-z0-9._]+$"))) {
                        binding.editLayoutUserName.error = "Username only contains a-z (lowercase),0 - 9, . and _"
                    } else {
                        isUsernameUnique(binding.editTextUserName.text.toString().trim()) { task ->
                            if (!task) {
                                binding.editLayoutUserName.error = "Username Already Exist"
                            } else {
                                binding.editLayoutUserName.error = null
                            }
                        }

                    }
                }
            }
        }
        // Setting up button event listeners
        setListeners()
    }


    private fun setListeners() {
        // Listener for the image holder (Rounded ImageView)
        binding.avatar.setOnClickListener {
            chooseImage() // Opens image picker when clicked
        }

        // Listener for the Sign Up button
        binding.buttonSignUp.setOnClickListener {
            isLoading(true) // Show loading state
            if (!isValidSignUp()) { // Validate signup input
                return@setOnClickListener
            } else {
                if (!binding.editTextUserName.text.toString().trim().matches(Regex("^[a-z0-9._]+$"))) {
                    binding.editLayoutUserName.error = "Username only contains a-z,0-9, . and _"
                    isLoading(false)
                } else {
                    isUsernameUnique(binding.editTextUserName.text.toString().trim()) { task ->
                        if (!task) {
                            binding.editLayoutUserName.error = "Username Already Exist"
                            isLoading(false)
                        } else {
                            signUp() // Proceed to sign up if valid
                        }
                    }

                }

            }
        }

        // Listener for the text view that redirects to Sign In
        binding.textViewSignIn.setOnClickListener {
            val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent) // Start Sign In activity
            finish() // Finish this activity
        }
    }

    private fun showToast(message: String) {
        // Show a toast message
        Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun chooseImage() {
        // Determine the appropriate permission needed based on Android version
        val permissionNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES // For Android 13+
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE // For Android 12 and below
        }
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                permissionNeeded
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(permissionNeeded), 1)
        } else {
            // Launch image picker if permission is granted
            imagePicker()
        }
    }

    private fun imagePicker() {
        // Create intent to pick an image
        val intent = Intent().apply {
            type = "image/*" // Specify the type as images
            action = Intent.ACTION_GET_CONTENT // Action to get content
        }
        // Launch the image picker
        activityResultLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Handle the result of the permission request
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // If permission is granted, call chooseImage again
                chooseImage()
            } else {
                // If denied, redirect user to app settings for manual permission granting
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                showToast("The app needs file permission to access images") // Inform user of the need for permission
            }
        }
    }

    private fun croppedImage(sourceUri: Uri, destinationUri: Uri) {
        // Start UCrop for cropping the image with specified aspect ratio and size
        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f) // Aspect ratio for cropping
            .withMaxResultSize(800, 800) // Max result size for cropped image
            .start(this@SignUpActivity) // Start the crop activity
    }

    private fun compressedImage(image: Bitmap, quality: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Compress image for Android 11 and above
            image.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, quality, outputStream)
        } else {
            // Compress image for Android 10 and below
            image.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
        }

        return outputStream.toByteArray() // Return the compressed image as byte array
    }

    private fun registerActivityResultLauncher() {
        // Register activity result launcher for image picking
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback { result ->
                    // If an image is picked
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        imageUri = result.data?.data // Get the image URI
                        imageUri?.let { uri ->
                            croppedImage(uri, destinationUri) // Crop the image
                        }
                    }
                })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle result from the crop activity
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            data?.let {
                val cropped = UCrop.getOutput(it) // Get the cropped image URI

                cropped?.let { uri ->
                    // Decode the bitmap from the URI based on Android version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bitmap = ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                contentResolver,
                                uri
                            )
                        )
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }

                    // Clean up temporary file after cropping
                    val tempFile = File(uri.path)
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }

                    // Compress the image and store it as byte array
                    compressedImageAsByteArray = compressedImage(bitmap, 80)

                    // Load the compressed image into the ImageView using Glide
                    Glide.with(this@SignUpActivity)
                        .asBitmap()
                        .load(compressedImageAsByteArray)
                        .placeholder(R.drawable.vector_default_user_avatar) // Placeholder while loading
                        .into(binding.avatar) // Set the image to avatar view

                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            showToast("Cropping failed, Choose any other image")
        }
    }

    private fun isValidSignUp(): Boolean {
        if (binding.editTextUserName.text.isNullOrEmpty()) {
            isLoading(false)
            binding.editLayoutUserName.error = "Please fill in the username field"
            return false
        } else if (binding.editTextUserName.text.toString().length > 30) {
            isLoading(false)
            binding.editLayoutUserName.error = "Username should be within 30 characters"
            return false
        } else if (binding.editTextDisplayName.text.isNullOrEmpty()) {
            //if Username/Display name editText is empty
            isLoading(false)
            binding.editLayoutUserName.error = "Please fill the display name field"
            return false
        } else if (binding.editTextDisplayName.text.toString().length > 30) {
            //if Username/Display name editText is empty
            isLoading(false)
            binding.editLayoutUserName.error = "Display Name should be within 30 characters"
            return false
        } else if (binding.editTextEmail.text.isNullOrEmpty()) {
            //if Email editText is empty
            isLoading(false)
            binding.editLayoutEmail.error = "Please fill the email field"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.editTextEmail.text.toString().lowercase().trim())
                .matches()
        ) {
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
        } else if (!binding.editTextPassword.text.toString().trim()
                .equals(binding.editTextConfirmPassword.text.toString().trim())
        ) {
            //if password and confirm password doesn't matches
            isLoading(false)
            binding.editLayoutEmail.error = null
            binding.editLayoutPassword.error = null
            binding.editTextConfirmPassword.error = "Password doesn't matches"
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
            binding.editTextEmail.text.toString().lowercase().trim(),
            binding.editTextPassword.text.toString().trim()
        )
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    //Trying to store the Display Image / Profile Picture of the user in the Firebase Cloud Storage
                    val imageRef = storage.child("avatar/${auth.currentUser!!.uid}")
                    if (compressedImageAsByteArray != null) {
                        imageRef.putBytes(compressedImageAsByteArray!!)
                            .addOnCompleteListener { uploadTask ->

                                if (uploadTask.isSuccessful) {
                                    imageRef.downloadUrl.addOnCompleteListener { urlTask ->

                                        if (urlTask.isSuccessful) {
                                            //Creating a hashmap of the userdata to store it in the Firebase Firestore
                                            val userDetails = hashMapOf(
                                                "userName" to binding.editTextUserName.text.toString(),
                                                "displayName" to binding.editTextDisplayName.text.toString(),
                                                "email" to binding.editTextEmail.text.toString(),
                                                "avatar" to urlTask.result.toString(),
                                                "typing" to false,
                                                "status" to "Offline",
                                                "lastHeartBeat" to "",
                                                "lastSeenAndOnlineSetting" to true
                                            )

                                            //Trying to store the userdata in the Firebase Firestore
                                            rtDB.getReference("users").child(auth.currentUser!!.uid)
                                                .setValue(userDetails)
                                                .addOnCompleteListener { databaseTask ->
                                                    if (databaseTask.isSuccessful) {
                                                        isLoading(false)
                                                        showToast("Signed Up Successfully")

                                                        //Sending the user back to Sign In activity
                                                        val intent = Intent(
                                                            this@SignUpActivity,
                                                            SignInActivity::class.java
                                                        )
                                                        intent.putExtra("fromSignUp", true)
                                                        startActivity(intent)
                                                        finish()

                                                    } else {
                                                        isLoading(false)
                                                        showToast("Failed to save user data")
                                                        Log.e(
                                                            "SignUpError",
                                                            databaseTask.exception.toString()
                                                        )
                                                    }
                                                }
                                        } else {
                                            isLoading(false)
                                            showToast("Failed to get download URL")
                                            Log.e("SignUpError", urlTask.exception.toString())
                                        }
                                    }
                                } else {
                                    isLoading(false)
                                    showToast("Failed to upload image")
                                    Log.e("SignUpError", uploadTask.exception.toString())
                                }
                            }
                    } else {
                        //Creating a hashmap of the userdata to store it in the Firebase Firestore
                        val userDetails = hashMapOf(
                            "userName" to binding.editTextUserName.text.toString(),
                            "displayName" to binding.editTextDisplayName.text.toString(),
                            "email" to binding.editTextEmail.text.toString().lowercase().trim(),
                            "avatar" to "",
                            "typing" to false,
                            "status" to "offline",
                            "lastHeartBeat" to "",
                            "lastSeenAndOnlineSetting" to true
                        )

                        //Trying to store the userdata in the Firebase Firestore
                        rtDB.getReference("users").child(auth.currentUser!!.uid)
                            .setValue(userDetails)
                            .addOnCompleteListener { databaseTask ->
                                if (databaseTask.isSuccessful) {
                                    isLoading(false)
                                    showToast("Signed Up Successfully")

                                    //Sending the user back to Sign In activity
                                    auth.signOut()
                                    val intent =
                                        Intent(this@SignUpActivity, SignInActivity::class.java)
                                    intent.putExtra("fromSignUp", true)
                                    startActivity(intent)
                                    finish()

                                } else {
                                    isLoading(false)
                                    showToast("Failed to save user data")
                                    Log.e("SignUpError", databaseTask.exception.toString())
                                }
                            }
                    }

                } else {
                    isLoading(false)

                    if (authTask.exception is FirebaseAuthException) {
                        when ((authTask.exception as FirebaseAuthException).errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> {
                                binding.editLayoutEmail.error = "Email is Already In Use"
                            }

                            "ERROR_WEAK_PASSWORD" -> {
                                binding.editLayoutPassword.error = "Weak Password, Use Another"
                            }

                            "ERROR_INVALID_EMAIL" -> {
                                binding.editLayoutEmail.error = "Invalid Email"
                            }

                            else -> {
                                showToast("Authentication failed")
                            }

                        }
                    }
                }
            }
    }

    private fun isUsernameUnique(username: String, callback: (Boolean) -> (Unit)) {
        rtDB.getReference("users").orderByChild("userName")
            .equalTo(username)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.exists()) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }
}
