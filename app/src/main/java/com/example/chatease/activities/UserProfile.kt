package com.example.chatease.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.R
import com.example.chatease.databinding.ActivityUserProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso
import kotlin.math.sign

class UserProfile : AppCompatActivity() {
    private val db = Firebase.firestore
    var userId : String = ""
    private lateinit var binding : ActivityUserProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar =  binding.userProfileActivityToolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "User Profile"

        userId = intent.getStringExtra("id")?:""

        db.collection("users").document(userId).get()
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val userName = task.result.getString("username")?:""
                    val displayName = task.result.getString("displayName")?:"Souvicks"
                    val userAvatar = task.result.getString("displayImage")?:""
                    val userBio = task.result.getString("userBio")?:""

                    Picasso.get().load(userAvatar).into(binding.userProfilePic)
                    binding.userName.text = userName
                    binding.displayName.text = displayName
                    binding.textViewBioText.text = userBio


                }
        }
        val userFromChatActivity = intent.getBooleanExtra("userFromChatActivity",false)
        if(userFromChatActivity){
                binding.messageUserButton.visibility = View.GONE
        }
        else{
            binding.messageUserButton.setOnClickListener {

            }
        }

        toolbar.setNavigationOnClickListener {
                onBackStackToChatActivity()
        }
    }

//    override fun onBackPressed() {
//        onBackStackToChatActivity()
//    }
    fun onBackStackToChatActivity(){
        val intent = Intent()
        intent.apply {
            putExtra("id",userId)
        }
        setResult(RESULT_OK,intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { // adding drop down menu option to the user profile
        menuInflater.inflate(R.menu.menu_chat_options, menu)
        return true
    }
}

