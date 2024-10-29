package com.example.chatease.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.R
import com.example.chatease.SearchUserAdapter
import com.example.chatease.SearchUserData
import com.example.chatease.databinding.ActivitySearchBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.firestore

class SearchActivity : AppCompatActivity() {
    // Object of Firebase Firestore
    val db = Firebase.firestore
    lateinit var recyclerView: RecyclerView

    // The Handler is used here to set a timeout mechanism to stop spamming API Calls
    // It's explained below where the handler is used
    private val handler = Handler(Looper.getMainLooper())

    // The variable which hold the time value of timeout to use in handler
    private val delay = 500L
    lateinit var binding: ActivitySearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recyclerView = findViewById(R.id.recyclerViewSearch)

        //Setting Custom toolbar and using the toolbar as editText to Search Users
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Basic RecyclerView things
        recyclerView.layoutManager = LinearLayoutManager(this@SearchActivity)

        // A MutableList of type SearchUserData which is a data class (Think of it as a custom object)
        // to store all the users objects (Think of it as a List of Objects)
        val searchUserList = mutableListOf<SearchUserData>()

        val adapter = SearchUserAdapter(searchUserList)
        recyclerView.adapter = adapter

        //A event listener of editText which tracks the change of characters
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                //Removing the extra space if the user typed it by mistake or a empty Search happens
                var query = text.toString().trim()
                //Removing all the timeout which was set previously using handler
                handler.removeCallbacksAndMessages(null)
                // If the user has typed something to search
                if (query.isNotEmpty()) {

                    // Below the handler is used to set the timeout, Everytime the user types the timeout resets.

                    // here is the workflow : (Suppose a user is typing)
                    // S                      (The event listener invokes)
                    // So                     (The event listener invokes)
                    // Sou                    (The event listener invokes)
                    // Souv                   (The event listener invokes)
                    // Souvi                  (The event listener invokes)
                    // Souvic                 (The event listener invokes)
                    // Souvick                (The event listener invokes)

                    // As the example shows a api spam is very likely to happen each time user types of delete characters
                    // to fix this I've implemented a mechanism, when the user types... a countdown starts
                    // (the countdown is denoted by "Delay" variable) Suppose its 5 seconds
                    // Each time the user types when the countDown is still going, the countDown resets back to 5 seconds
                    // countDown Reset happens in line number 68
                    // When the countDown is over, the Search api calls gets sent to the database

                    handler.postDelayed({
                        if (binding.editTextSearch.text.toString().isNotEmpty()) {
                            // When the search happens the progressBar gets visible
                            binding.progressBar.visibility = View.VISIBLE

                            if (query.startsWith("@")) {
                                //if the search is Global which is indicated by using @
                                // e.g : @Goku

                                query = query.split("@")[1].trim()
                                val upperBoundQuery = query + "\uf8ff"
                                if (query.isEmpty()) {
                                    // If query is empty, progress bar gets invisible
                                    binding.progressBar.visibility = View.INVISIBLE
                                } else {
                                    db.collection("users")
                                        .whereGreaterThanOrEqualTo("username", query)
                                        .whereLessThan("username", upperBoundQuery)
                                        .get()
                                        .addOnCompleteListener { search ->
                                            searchUserList.clear()
                                            // If Search is successful, progress bar gets invisible
                                            binding.progressBar.visibility = View.INVISIBLE
                                            // if we found the users who matched the query
                                            if (search.isSuccessful && search.result.size() > 0) {
                                                for (document in search.result) {
                                                    val userID = document.id
                                                    val userName = document.getString("username") ?: ""
                                                    val userAvatar = document.getString("displayImage") ?: ""
                                                    // Creating the user object
                                                    val userProfile = SearchUserData(userName, userID, userAvatar)
                                                    // Adding the user object to the MutableList of UserObject
                                                    searchUserList.add(userProfile)
                                                }

                                                // This is a custom function which is defined in the Adapter Class to detect
                                                // whether the search happened or not
                                                adapter.updateSearchState(true)

                                            } else {

                                                // This is a custom function which is defined in the Adapter Class to detect
                                                // whether the search happened or not
                                                adapter.updateSearchState(false)
                                            }

                                            // This function is a predefined function which tells the adapter to update
                                            // it's data in the recycleView
                                            adapter.notifyDataSetChanged()
                                        }
                                }
                            } else {
                                //if the search is Local which is indicated by not using @
                                // e.g : Goku
                                binding.progressBar.visibility = View.INVISIBLE

                            }
                        }
                    }, delay)
                } else {

                    // If the user has removed the previously typed characters to search
                    // Clearing the MutableList of UserObject below
                    searchUserList.clear()

                    // This is a custom function which is defined in the Adapter Class to detect
                    // whether the search happened or not
                    adapter.updateSearchState(false)

                    // This function is a predefined function which tells the adapter to update
                    // it's data in the recycleView
                    adapter.notifyDataSetChanged()
                }
            }
        })

    }
}



