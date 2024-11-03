package com.example.chatease.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import com.example.chatease.databinding.ActivitySettingsBinding
import com.example.chatease.dataclass.UserDataSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class SettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingsBinding
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        registerActivityResultLauncher()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = binding.userProfileActivityToolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        var userName = ""
        var userAvatar = ""
        var userDisplayName = ""
        var userBio = ""

        val userId = auth.currentUser?.uid ?: ""

        db.collection("users").document(userId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userName = task.result.getString("username") ?: ""
                    userAvatar = task.result.getString("displayImage") ?: ""
                    userDisplayName = task.result.getString("displayName") ?: "ChangeFromKotlin"
                    userBio = task.result.getString("userBio") ?: "ChangeFromKotlin"

                    Picasso.get().load(userAvatar).into(binding.userAvatar)
                    binding.editTextUserName.setText(userName)
                    binding.editTextDisplayName.setText(userDisplayName)
                    binding.editTextUserBio.setText(userBio)


                }
            }

        binding.applyChangesButton.setOnClickListener {
            binding.applyButtonProgressBar.visibility = View.VISIBLE
            if (binding.editTextUserBio.text.isNotEmpty()
                && binding.editTextUserName.text.isNotEmpty()
                && binding.editTextDisplayName.text.isNotEmpty()
            ) {
                var isChanged = false
                if (binding.editTextDisplayName.text.toString() != userDisplayName) {
                    isChanged = true
                } else if (binding.editTextUserBio.text.toString() != userBio) {
                    isChanged = true
                } else if (binding.editTextUserName.text.toString() != userName) {
                    isChanged = true
                }

                if (isChanged || imageUri != null) {
                    if(imageUri!=null) {
                        val imageRef = storage.child("displayImage/$userId")
                        imageRef.putFile(imageUri!!).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                imageRef.downloadUrl.addOnCompleteListener { task1 ->
                                    if (task1.isSuccessful) {
                                        userAvatar = task1.result.toString()
                                    }
                                }
                            }
                        }

                    }

                    val userDataObject = UserDataSettings(
                        username = binding.editTextUserName.text.toString(),
                        displayName = binding.editTextDisplayName.text.toString(),
                        userBio = binding.editTextUserBio.text.toString(),
                        displayImage = userAvatar
                    )

                    db.collection("users").document(userId)
                        .set(userDataObject)
                        .addOnCompleteListener { task ->
                            if(task.isSuccessful){
                                binding.applyButtonProgressBar.visibility = View.GONE
                                Toast.makeText(this,"Successfully Updated",Toast.LENGTH_SHORT).show()
                            }
                            else{
                                binding.applyButtonProgressBar.visibility = View.GONE
                                Toast.makeText(this,"Failed To Update Data",Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }

        binding.signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this@SettingsActivity, SignInActivity::class.java))
        }

        binding.frameUserAvatar.setOnClickListener {
            chooseImage()
        }
    }

    private fun chooseImage() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        } else {
            val intent = Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
            }
            activityResultLauncher.launch(intent)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseImage()
            } else {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.apply {
                    data = Uri.parse("package:$packageName")
                }
                Toast.makeText(
                    this,
                    "Storage Permission is Required for Updating Avatar",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(intent)
            }
        }
    }


    fun registerActivityResultLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback { result ->
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        imageUri = result.data?.data
                    }
                    imageUri?.let {
                        Picasso.get().load(it).into(binding.userAvatar)
                    }
                })
    }
}