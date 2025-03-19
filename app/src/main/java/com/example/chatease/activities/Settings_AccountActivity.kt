package com.example.chatease.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.chatease.R
import com.example.chatease.databinding.ActivitySettingAccountBinding
import com.example.chatease.dataclass.UserDataSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

class Settings_AccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingAccountBinding // View binding for accessing UI elements
    private val rtDB =
        FirebaseDatabase.getInstance() // Firebase Realtime Database database reference
    private val auth = FirebaseAuth.getInstance() // Firebase Authentication instance
    private val storage = FirebaseStorage.getInstance().reference // Firebase Storage reference
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent> // Activity result launcher for image picking
    private var imageUri: Uri? = null // Uri for the selected image
    private lateinit var bitmap: Bitmap // Bitmap representation of the image
    private var compressedImageAsByteArray: ByteArray? =
        null // Compressed image as byte array for upload
    private lateinit var userName: String // User's username
    private lateinit var userAvatar: String // User's avatar URL
    private lateinit var userDisplayName: String // User's display name
    private lateinit var userBio: String // User's bio
    private lateinit var userId: String // User's unique ID
    private lateinit var destinationUri: Uri // Destination URI for cropped image
    private lateinit var databaseReference: DatabaseReference
    private var valueListener: ValueEventListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        registerActivityResultLauncher() // Registering activity result launcher for image picking
        binding = ActivitySettingAccountBinding.inflate(layoutInflater) // Initializing view binding
        super.onCreate(savedInstanceState)
        setContentView(binding.root) // Setting content view to the binding root

        // Adjusting padding for system UI (like status bar and navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = binding.userProfileActivityToolbar // Setting up the toolbar
        setSupportActionBar(toolbar) // Setting the toolbar as the app bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enabling the back button
        supportActionBar?.setDisplayShowTitleEnabled(false)// Setting title for the toolbar

        userId = auth.currentUser?.uid ?: "" // Getting the current user's ID

        // Setting destination URI for cropped image
        destinationUri = Uri.fromFile(File(cacheDir, "temp_cropped_image.webp"))

        // Fetching user data from Firestore
        databaseReference = rtDB.getReference("users").child(userId)
        databaseReference.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Retrieving user data from Firestore
                userName = snapshot.child("userName").getValue(String::class.java) ?: ""
                userAvatar = snapshot.child("avatar").getValue(String::class.java) ?: ""
                userDisplayName =
                    snapshot.child("displayName").getValue(String::class.java) ?: ""
                userBio = snapshot.child("userBio").getValue(String::class.java) ?: ""

                // Loading user avatar into ImageView using Glide

                if (!isFinishing && !isDestroyed) {
                    Glide.with(this@Settings_AccountActivity)
                        .load(userAvatar)
                        .placeholder(R.drawable.vector_default_user_avatar) // Placeholder image while loading
                        .into(binding.userAvatar)

                }

                // Setting text fields with user data
                binding.editTextUserName.setText(userName)
                binding.editTextDisplayName.setText(userDisplayName)
                binding.editTextUserBio.setText(userBio)
            }
        }
//        valueListener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//
//            }
//
//        }

//        valueListener?.let {
//            databaseReference.addValueEventListener(it)
//        }

        // Setting onClickListener for apply changes button
        binding.applyChangesButton.setOnClickListener {
            binding.applyButtonProgressBar.visibility =
                View.VISIBLE // Show progress bar while applying changes
            binding.applyChangesButton.visibility = View.INVISIBLE
            // Check if any field is not empty
            if (binding.editTextUserName.text!!.isNotEmpty() && binding.editTextDisplayName.text!!.isNotEmpty()) {
                var isChanged = false // Flag to check if any data has changed

                CoroutineScope(Dispatchers.Main).launch {
                    // Checking if any user data has changed
                    if (binding.editTextUserName.text.toString() != userName) {
                        isChanged = true

                        val isUnique =
                            async { isUsernameUnique(binding.editTextUserName.text.toString()) }.await()
                        if (!isUnique) {
                            binding.textInputLayoutUserName.error = "Username needs to be unique"
                            binding.applyButtonProgressBar.visibility = View.INVISIBLE
                            binding.applyChangesButton.visibility = View.VISIBLE
                            return@launch
                        }

                        binding.textInputLayoutUserName.error = null


                    } else if (binding.editTextDisplayName.text.toString() != userDisplayName) {
                        isChanged = true
                    } else if (binding.editTextUserBio.text.toString() != userBio) {
                        isChanged = true
                    }

                    if (isChanged) {
                        if (binding.editTextUserName.text.toString().length > 30) {
                            binding.textInputLayoutUserName.error =
                                "Username Must Be Within 30 Characters"
                            binding.applyButtonProgressBar.visibility = View.INVISIBLE
                            binding.applyChangesButton.visibility = View.VISIBLE
                            return@launch
                        } else if (!binding.editTextUserName.text.toString()
                                .matches(Regex("^[a-z0-9_.]+$"))
                        ) {
                            binding.textInputLayoutUserName.error = "Username Must be in Lowercase"
                            binding.applyButtonProgressBar.visibility = View.INVISIBLE
                            binding.applyChangesButton.visibility = View.VISIBLE
                            return@launch
                        } else if (binding.editTextDisplayName.text.toString().length > 30) {
                            binding.textInputLayoutDisplayName.error =
                                "Display Name Must Be Within 30 Characters"
                            binding.applyButtonProgressBar.visibility = View.INVISIBLE
                            binding.applyChangesButton.visibility = View.VISIBLE
                            return@launch
                        } else if (binding.editTextUserBio.text.toString().length > 100) {
                            binding.textInputLayoutUserBio.error =
                                "Bio Must Be Within 100 Characters"
                            binding.applyButtonProgressBar.visibility = View.INVISIBLE
                            binding.applyChangesButton.visibility = View.VISIBLE
                            return@launch
                        }
                    }

                    // If data has changed or a new image is selected, update the database
                    if (isChanged || (imageUri != null)) {
                        updateTheDataInTheDatabase() // Call to update user data in Firestore

                    } else {
                        // Notify user if there are no changes to apply
                        Toast.makeText(
                            this@Settings_AccountActivity,
                            "Successfully Updated",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        binding.applyChangesButton.visibility = View.VISIBLE
                        elementsClickabilityToggler(false)
                        isEditButtonClicked = false
                        binding.textInputLayoutUserName.error = null
                        binding.textInputLayoutDisplayName.error = null
                        binding.textInputLayoutUserBio.error = null
                    }
                }

            } else {
                Toast.makeText(
                    this@Settings_AccountActivity,
                    "Please fill the username & display name field",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

//    override fun onDestroy() {
//        Log.e("Testing", "Checking")
//        super.onDestroy()
////        valueListener?.let {
////            databaseReference.removeEventListener(it)
////        }
//    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_info, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private var isEditButtonClicked = false
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.editInfoIcon -> {
                if (isEditButtonClicked) {
                    elementsClickabilityToggler(false)
                    isEditButtonClicked = false
                } else {
                    elementsClickabilityToggler(true)
                    isEditButtonClicked = true
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun elementsClickabilityToggler(state: Boolean) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        binding.editTextDisplayName.apply {
            isFocusable = state
            isFocusableInTouchMode = state
        }
        binding.editTextUserName.apply {
            isFocusable = state
            isFocusableInTouchMode = state
        }
        binding.editTextUserBio.apply {
            isFocusable = state
            isFocusableInTouchMode = state
        }

        if (state) {
            binding.editTextDisplayName.requestFocus() // Request focus programmatically
            inputMethodManager.showSoftInput(binding.editTextDisplayName, InputMethodManager.SHOW_IMPLICIT) // Show the keyboard
            binding.frameLayoutApplyButton.visibility = View.VISIBLE
            binding.frameChangeAvatarIcon.visibility = View.VISIBLE
            // Setting onClickListener for avatar frame to choose an image
            binding.frameUserAvatar.setOnClickListener {
                chooseImage() // Call to choose image from gallery
            }
        } else {
            inputMethodManager.hideSoftInputFromWindow(binding.editTextDisplayName.windowToken, 0)
            binding.frameUserAvatar.setOnClickListener(null)
            binding.editTextDisplayName.clearFocus() // clears focus programmatically
            binding.editTextUserName.clearFocus() // clears focus programmatically
            binding.editTextUserBio.clearFocus() // clears focus programmatically

            binding.frameLayoutApplyButton.visibility = View.INVISIBLE
            binding.frameChangeAvatarIcon.visibility = View.INVISIBLE

            if (!isFinishing && !isDestroyed) {
                Glide.with(this@Settings_AccountActivity)
                    .load(userAvatar)
                    .placeholder(R.drawable.vector_default_user_avatar) // Placeholder image while loading
                    .into(binding.userAvatar)

            }

            // Setting text fields with user data
            binding.editTextUserName.setText(userName)
            binding.editTextDisplayName.setText(userDisplayName)
            binding.editTextUserBio.setText(userBio)
        }
    }

    private suspend fun isUsernameUnique(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { coroutine ->
                rtDB.getReference("users").orderByChild("userName")
                    .equalTo(username)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful && !task.result.exists()) {
                            coroutine.resume(true)
                        } else {
                            coroutine.resume(false)
                        }
                    }
            }
        }
    }

    private fun chooseImage() {
        // Determine the appropriate permission needed based on the Android version
        val permissionNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API level 33) and above require READ_MEDIA_IMAGES for images
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            // Android 12 (API level 31) and below use READ_EXTERNAL_STORAGE for media access
            android.Manifest.permission.READ_EXTERNAL_STORAGE
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
            // Open image picker if permission is granted
            imagePicker()
        }
    }

    private fun imagePicker() {
        // Creating an intent to pick an image from the gallery
        val intent = Intent().apply {
            type = "image/*" // Setting the type to image
            action = Intent.ACTION_GET_CONTENT // Action to get content
        }
        activityResultLauncher.launch(intent) // Launch the image picker
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) { // Check if it's our permission request
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, call chooseImage again
                chooseImage()
            } else {
                // Show settings screen if permission is denied
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.apply {
                    data = Uri.parse("package:$packageName") // Open app settings
                }
                Toast.makeText(
                    this,
                    "Storage Permission is Required for Updating Avatar", // Notify user
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(intent) // Start settings intent
            }
        }
    }

    private fun registerActivityResultLauncher() {
        // Registering the activity result launcher to handle image crop result
        activityResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result -> // Callback for result
                if (result.resultCode == RESULT_OK && result.data != null) {
                    imageUri = result.data?.data // Get the selected image URI

                    imageUri?.let { uri ->
                        // Start cropping the selected image
                        croppedImage(uri, destinationUri)
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle the result of the image cropping
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            data?.let {
                val cropped = UCrop.getOutput(it) // Get the cropped image URI

                cropped?.let { uri ->
                    // Decode the image based on Android version
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

                    // Deleting the temporary file which was used to store the cropped image generated by Ucrop Library
                    val tempFile = File(uri.path)
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }

                    compressedImageAsByteArray = compressedImage(bitmap, 80)
                    Glide.with(this@Settings_AccountActivity)
                        .asBitmap()
                        .load(compressedImageAsByteArray)
                        .placeholder(R.drawable.vector_default_user_avatar)
                        .into(binding.userAvatar)

                }
            }
        }
    }

    private fun croppedImage(sourceUri: Uri, destinationUri: Uri) {
        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .start(this@Settings_AccountActivity)
    }

    private fun compressedImage(image: Bitmap, quality: Int): ByteArray {
//        val resizeImageBitmap = resizeBitmap(image,800,800)
        val outputStream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above
            image.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, quality, outputStream)
        } else {
            // For Android 10 (API 29) and below
            image.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
        }

        return outputStream.toByteArray()
    }

    private fun updateTheDataInTheDatabase() {
        if (compressedImageAsByteArray != null) {
            val imageRef = storage.child("avatar/$userId")
            imageRef.putBytes(compressedImageAsByteArray!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        imageRef.downloadUrl.addOnCompleteListener { task1 ->
                            if (task1.isSuccessful) {
                                userAvatar = task1.result.toString()
                                // Proceed to update Firestore with user data after image is uploaded
                                updateUserDataInFirestore()
                            } else {
                                // Handle failure to get download URL
                                Toast.makeText(
                                    this,
                                    "Failed to retrieve image URL",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        // Handle upload failure
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // No new image, just update user data in Firestore
            updateUserDataInFirestore()
        }
    }

    private fun updateUserDataInFirestore() {
        val userDataObject = UserDataSettings(
            userName = binding.editTextUserName.text.toString(),
            displayName = binding.editTextDisplayName.text.toString(),
            userBio = binding.editTextUserBio.text.toString(),
            avatar = userAvatar
        )

        rtDB.getReference("users").child(userId).updateChildren(
            mapOf(
                "userName" to binding.editTextUserName.text.toString(),
                "displayName" to binding.editTextDisplayName.text.toString(),
                "userBio" to binding.editTextUserBio.text.toString(),
                "avatar" to userAvatar
            )
        )
            .addOnSuccessListener {
                Toast.makeText(this, "Successfully Updated", Toast.LENGTH_LONG).show()
                binding.applyButtonProgressBar.visibility = View.INVISIBLE
                binding.applyChangesButton.visibility = View.VISIBLE
                elementsClickabilityToggler(false)
                isEditButtonClicked = false
                // Update UI with new data if necessary
                binding.textInputLayoutUserName.error = null
                binding.textInputLayoutDisplayName.error = null
                binding.textInputLayoutUserBio.error = null
                userName = binding.editTextUserName.text.toString()
                userDisplayName = binding.editTextDisplayName.text.toString()
                userBio = binding.editTextUserBio.text.toString()
                if (!isFinishing && !isDestroyed) {
                    Glide.with(this@Settings_AccountActivity)
                        .load(userAvatar)
                        .placeholder(R.drawable.vector_default_user_avatar) // Placeholder image while loading
                        .into(binding.userAvatar)

                }

                // Setting text fields with user data
                binding.editTextUserName.setText(userName)
                binding.editTextDisplayName.setText(userDisplayName)
                binding.editTextUserBio.setText(userBio)
            }.addOnFailureListener { e ->
                // Handle failure to update Firestore
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.applyButtonProgressBar.visibility = View.INVISIBLE
                binding.applyChangesButton.visibility = View.VISIBLE
            }

    }
}