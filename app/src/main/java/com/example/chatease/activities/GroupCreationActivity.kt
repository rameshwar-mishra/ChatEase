package com.example.chatease.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
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
import com.example.chatease.databinding.ActivityGroupCreationBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class GroupCreationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupCreationBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null // Uri for the selected image
    private lateinit var destinationUri: Uri
    private lateinit var bitmap: Bitmap // Bitmap representation of the image
    private var compressedImageAsByteArray: ByteArray? = null // Compressed image as byte array for upload
    private val rtDB = FirebaseDatabase.getInstance()

    // Firebase Storage reference for storing images
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val selectedParticipantsList = intent.getStringArrayListExtra("selectedParticipants") ?: arrayListOf<String>()

        val selectedParticipantsMap = selectedParticipantsList.associateWith { true }

//        val userDataList = intent.getSerializableExtra("userDataList") as ArrayList<UserData>

        destinationUri = Uri.fromFile(File(cacheDir, "temp_cropped_image.webp"))

        registerActivityResultLauncher()

        binding.frameGroupIcon.setOnClickListener {
            chooseImage()
        }
        binding.floatingActionButtonCreateGroup.setOnClickListener {
            createGroup(participantsMap = selectedParticipantsMap)
        }
    }

    private fun createGroup(participantsMap: Map<String, Boolean>) {
        if (binding.groupName.text.isNullOrEmpty()) {
            binding.groupName.error = "Need a group name"
            binding.groupName.requestFocus()
            return
        } else {
            // Reference to the group collection
            val groupRef = rtDB.getReference("groups")
            // Generating a new group ID
            val groupId = groupRef.push().key // Firebase generates a random ID
            if (groupId != null) {
                uploadImage(groupID = groupId, participantsMap = participantsMap)
            } else {
                Toast.makeText(this@GroupCreationActivity, "Failed to generate, try again", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadImage(groupID: String, participantsMap: Map<String, Boolean>) {
        CoroutineScope(Dispatchers.IO).launch {
            if (compressedImageAsByteArray != null) {
                val imageRef = storage.child("groupIcon/$groupID")
                imageRef.putBytes(compressedImageAsByteArray!!)
                    .addOnSuccessListener { snapshot ->
                        imageRef.downloadUrl.addOnCompleteListener { urlTask ->
                            if (urlTask.isSuccessful) {
                                uploadGroupData(groupID = groupID, iconURL = urlTask.result.toString(), participantsMap = participantsMap)
                            } else {
                                Toast.makeText(
                                    this@GroupCreationActivity,
                                    "Failed to upload the image, using the default icon instead",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                uploadGroupData(groupID = groupID, iconURL = null, participantsMap = participantsMap)
                            }
                        }
                    }
            } else {
                uploadGroupData(groupID = groupID, iconURL = null, participantsMap = participantsMap)
            }
        }
    }

    private fun uploadGroupData(groupID: String, iconURL: String?, participantsMap: Map<String, Boolean>) {
        CoroutineScope(Dispatchers.IO).launch {
            val groupDesc = if (binding.groupDesc.text.toString().isEmpty()) {
                null
            } else {
                binding.groupDesc.text.toString()
            }

            rtDB.getReference("groups/${groupID}/metadata").setValue(
                mapOf(
                    "groupName" to binding.groupName.text.toString(),
                    "groupDesc" to groupDesc,
                    "createdAt" to ServerValue.TIMESTAMP,
                    "groupIcon" to iconURL,
                    "participants" to participantsMap
                )
            )
                .addOnSuccessListener {
                    val intent  = Intent(this@GroupCreationActivity, GroupChatActivity::class.java)
                    intent.putExtra("groupID",groupID)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this@GroupCreationActivity, "Failed to create the group, try again", Toast.LENGTH_LONG).show()
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

                    val params = binding.groupIcon.layoutParams
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    binding.groupIcon.layoutParams = params
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