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
import android.view.ViewGroup
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
import com.example.chatease.databinding.ActivityGroupSettingsBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

    class GroupSettingsActivity : AppCompatActivity() {
        lateinit var binding: ActivityGroupSettingsBinding
        private val rtDB =
            FirebaseDatabase.getInstance() // Firebase Realtime Database database reference
        private val storage = FirebaseStorage.getInstance().reference // Firebase Storage reference
        private lateinit var activityResultLauncher: ActivityResultLauncher<Intent> // Activity result launcher for image picking
        private var imageUri: Uri? = null // Uri for the selected image
        private lateinit var bitmap: Bitmap // Bitmap representation of the image
        private var compressedImageAsByteArray: ByteArray? =
            null // Compressed image as byte array for upload
        private lateinit var destinationUri: Uri // Destination URI for cropped image
        private var isGroupSettingDataGettingSaved = false
        private var groupIcon: String? = ""
        private var groupName: String = ""
        private var groupDesc: String? = ""
        private var groupId: String = ""
        private var modifiedGroupName: Boolean = false
        private var modifiedGroupDesc: Boolean = false
        private var modifiedGroupIcon: Boolean = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            registerActivityResultLauncher() // Registering activity result launcher for image picking
            binding = ActivityGroupSettingsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            destinationUri = Uri.fromFile(File(cacheDir, "temp_cropped_image.webp"))

            groupIcon = intent.getStringExtra("groupIcon") ?: ""
            groupName = intent.getStringExtra("groupName") ?: ""
            groupDesc = intent.getStringExtra("groupDesc") ?: ""
            groupId = intent.getStringExtra("groupID") ?: ""
            if (!groupIcon.isNullOrEmpty()) {
                if (!isFinishing && !isDestroyed) {
                    Glide.with(this)
                        .load(groupIcon)
                        .placeholder(R.drawable.vector_icon_group)
                        .into(binding.groupIcon)
                }
            }

            binding.editTextGroupName.setText(groupName)
            binding.editTextGroupDescription.setText(groupDesc)

            binding.applyChangesButton.setOnClickListener {
                editGroupData()
            }
        }

        override fun onCreateOptionsMenu(menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.menu_edit_info, menu)
            return super.onCreateOptionsMenu(menu)
        }

        private var isEditButtonClicked = false

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {

                R.id.editInfoIcon -> {
                    if (isEditButtonClicked) {
                        elementsClickabilityToggler(false)
                        isEditButtonClicked = false
                    } else {
                        elementsClickabilityToggler(true)
                        isEditButtonClicked = true
                    }
                    return true
                }

                android.R.id.home -> {
                    val intent = Intent()
                    if (modifiedGroupIcon) {
                        intent.putExtra("modifiedGroupIcon", groupIcon)
                    }
                    if (modifiedGroupName) {
                        intent.putExtra("modifiedGroupName", groupName)
                    }
                    if (modifiedGroupDesc) {
                        intent.putExtra("modifiedGroupDesc", groupDesc)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                    return true
                }

                else -> return super.onOptionsItemSelected(item)
            }
        }

        private fun elementsClickabilityToggler(state: Boolean) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

            binding.editTextGroupName.apply {
                isFocusable = state
                isFocusableInTouchMode = state
            }
            binding.editTextGroupDescription.apply {
                isFocusable = state
                isFocusableInTouchMode = state
            }

            if (state) {
                binding.editTextGroupName.requestFocus() // Request focus programmatically
                inputMethodManager.showSoftInput(binding.editTextGroupName, InputMethodManager.SHOW_IMPLICIT) // Show the keyboard
                binding.frameLayoutApplyButton.visibility = View.VISIBLE
                binding.frameChangeAvatarIcon.visibility = View.VISIBLE

                // Setting onClickListener for avatar frame to choose an image
                binding.groupIcon.setOnClickListener {
                    chooseImage() // Call to choose image from gallery
                }
            } else {
                binding.groupIcon.setOnClickListener(null)
                binding.editTextGroupName.clearFocus()
                binding.editTextGroupDescription.clearFocus()
                inputMethodManager.hideSoftInputFromWindow(binding.editTextGroupName.windowToken, 0) // Hide the keyboard

                binding.frameLayoutApplyButton.visibility = View.INVISIBLE
                binding.frameChangeAvatarIcon.visibility = View.INVISIBLE

                if (!isFinishing && !isDestroyed) {
                    Glide.with(this@GroupSettingsActivity)
                        .load(groupIcon)
                        .placeholder(R.drawable.vector_icon_group) // Placeholder image while loading
                        .into(binding.groupIcon)
                }
                // Setting text fields with user data
                binding.editTextGroupName.setText(groupName)
                binding.editTextGroupDescription.setText(groupDesc)
            }
        }

        private fun editGroupData() {
            if (binding.editTextGroupName.text.isNullOrEmpty()) {
                binding.editTextGroupName.error = "Need a group name"
                binding.editTextGroupName.requestFocus()
                return
            } else {
                // Reference to the group collection
                // Generating a new group ID

                if (!isGroupSettingDataGettingSaved) {
                    isGroupSettingDataGettingSaved = true
                    applyingChanges(true)
                    if (groupId != null) {
                        uploadImage(groupID = groupId)
                    } else {
                        isGroupSettingDataGettingSaved = false
                        Toast.makeText(
                            this@GroupSettingsActivity,
                            "Failed to generate Group ID, try again",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        private fun applyingChanges(buttonState: Boolean) {
            if (buttonState) {
                binding.applyChangesButton.visibility = ViewGroup.GONE
                binding.applyButtonProgressBar.visibility = ViewGroup.VISIBLE
            } else {
                binding.applyChangesButton.visibility = ViewGroup.VISIBLE
                binding.applyButtonProgressBar.visibility = ViewGroup.GONE
            }
        }

        private fun uploadImage(groupID: String) {
            CoroutineScope(Dispatchers.IO).launch {
                if (compressedImageAsByteArray != null) {
                    val imageRef = storage.child("groupIcon/$groupID")
                    imageRef.putBytes(compressedImageAsByteArray!!)
                        .addOnSuccessListener {
                            imageRef.downloadUrl.addOnCompleteListener { urlTask ->
                                if (urlTask.isSuccessful) {
                                    uploadGroupData(
                                        groupID = groupID,
                                        iconURL = urlTask.result.toString(),
                                    )
                                } else {
                                    Toast.makeText(
                                        this@GroupSettingsActivity,
                                        "Failed to upload the image, using the default icon instead",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    uploadGroupData(
                                        groupID = groupID,
                                        iconURL = null,
                                    )
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this@GroupSettingsActivity,
                                "Failed to upload the image, using the default icon instead",
                                Toast.LENGTH_LONG
                            ).show()

                            uploadGroupData(
                                groupID = groupID,
                                iconURL = null,
                            )
                        }
                } else {
                    uploadGroupData(
                        groupID = groupID,
                        iconURL = null,
                    )
                }
            }
        }

        private fun uploadGroupData(
            groupID: String,
            iconURL: String?,
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val groupDescption = binding.editTextGroupDescription.text.toString().ifEmpty {
                    null
                }

                rtDB.getReference("groups/${groupID}/metadata").updateChildren(
                    mapOf(
                        "groupName" to binding.editTextGroupName.text.toString(),
                        "groupDesc" to groupDescption,
                        "groupIcon" to iconURL,
                    )
                )
                    .addOnSuccessListener {
                        if (groupIcon != iconURL) {
                            modifiedGroupIcon = true
                            groupIcon = iconURL
                        }

                        if (groupName != binding.editTextGroupName.text.toString().trim()) {
                            modifiedGroupName = true
                            groupName = binding.editTextGroupName.text.toString().trim()
                        }

                        if (groupDesc != groupDescption) {
                            modifiedGroupDesc = true
                            groupDesc = groupDescption
                        }

                        applyingChanges(false)
                        isGroupSettingDataGettingSaved = false

                        elementsClickabilityToggler(false)
                        isEditButtonClicked = false

                        Toast.makeText(
                            this@GroupSettingsActivity,
                            "Successfully saved the group data",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener {
                        applyingChanges(false)
                        isGroupSettingDataGettingSaved = false
                        Toast.makeText(
                            this@GroupSettingsActivity,
                            "Failed to saved the group data, try again",
                            Toast.LENGTH_LONG
                        ).show()
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

                        Glide.with(this)
                            .asBitmap()
                            .load(compressedImageAsByteArray)
                            .placeholder(R.drawable.vector_icon_group)
                            .into(binding.groupIcon)

    //                    val params = binding.groupIcon.layoutParams
    //                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
    //                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
    //                    binding.groupIcon.layoutParams = params
                    }
                }
            }
        }

        private fun croppedImage(sourceUri: Uri, destinationUri: Uri) {
            UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(800, 800)
                .start(this)
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

    }