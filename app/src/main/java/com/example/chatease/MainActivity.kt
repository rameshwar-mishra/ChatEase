package com.example.chatease

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatease.activities.SearchActivity
import com.example.chatease.activities.SignInActivity
import com.example.chatease.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
//        supportActionBar.title =

//      Floating Action Button (FAB) to open Search Activity
        binding.floatingActionButtonSearch.setOnClickListener {
            startActivity(Intent(this@MainActivity, SearchActivity::class.java))
        }
    }

    //  Inflating 3 Dots Toolbar Menu Buttons
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_signout, menu)
        return true
    }

    //  When one of the options get selected from the 3 Dots Toolbar Menu Buttons
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // When the sign out button is pressed
        if (item.itemId == R.id.signOut) {
            // The user gets signed out from the database
            auth.signOut()
            // Opens the Sign in Activity
            startActivity(Intent(this@MainActivity, SignInActivity::class.java))
            // Closes the Main Activity
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

}
