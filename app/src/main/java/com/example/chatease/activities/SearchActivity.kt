package com.example.chatease.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.R
import com.example.chatease.recyclerview_adapters.SearchUserAdapter
import com.example.chatease.dataclass.SearchUserData
import com.example.chatease.databinding.ActivitySearchBinding
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore

class SearchActivity : AppCompatActivity() {
    // Initialize Firestore database instance
    private val db = Firebase.firestore
    private val rtDB = FirebaseDatabase.getInstance()

    lateinit var recyclerView: RecyclerView
    // Handler to manage search delay to enhance performance and prevent redundant queries
    private val handler = Handler(Looper.getMainLooper())
    private val delay = 500L // 500 milliseconds delay for search queries
    lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup view binding to access layout views efficiently
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable padding to prevent UI elements from overlapping with system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup the RecyclerView for displaying search results
        recyclerView = findViewById(R.id.recyclerViewSearch)
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar) // Set custom toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button on toolbar

        recyclerView.layoutManager = LinearLayoutManager(this@SearchActivity) // Set layout manager
        val searchUserList = mutableListOf<SearchUserData>() // List to hold user search results
        val adapter = SearchUserAdapter(this@SearchActivity, searchUserList) // Initialize adapter for RecyclerView
        recyclerView.adapter = adapter // Set the adapter to RecyclerView

        // Add a text watcher to listen for changes in search input
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            // Handle input after the text has changed
            override fun afterTextChanged(text: Editable?) {
                var query = text.toString().trim() // Get trimmed search query
                handler.removeCallbacksAndMessages(null) // Clear previous search delay to prevent spam

                // Implement a delay mechanism to prevent excessive API calls when typing
                // This helps in cases where users might type quickly, resetting the query timer each time

                if (query.isNotEmpty()) {
                    // Delay search execution to avoid rapid successive queries
                    handler.postDelayed({
                        // Check if the search field still contains text
                        if (binding.editTextSearch.text.toString().isNotEmpty()) {
                            binding.progressBar.visibility = View.VISIBLE // Show loading indicator

                            if (query.startsWith("@")) {
                                // Indicates a global search (e.g., "@Goku")
                                query = query.split("@")[1].trim() // Remove '@' prefix from query
                                val upperBoundQuery = query + "\uf8ff" // Define upper bound for query range

                                if (query.isNotEmpty()) {
                                    // Perform Firestore query for user search
                                    rtDB.getReference("users").orderByChild("userName")
                                        .startAt(query)
                                        .endAt(upperBoundQuery)
                                        .get()
                                        .addOnCompleteListener { search ->
                                            binding.progressBar.visibility = View.INVISIBLE // Hide loading indicator
                                            searchUserList.clear() // Clear previous search results

                                            if (search.isSuccessful ) {
                                                // Loop through results and add to the list if found

                                                for (document in search.result.children) {
                                                    val userID = document.key ?: "" // Get user ID
                                                    val userName = document.child("userName").getValue(String::class.java) ?: "" // Get username
                                                    val displayName = document.child("displayName").getValue(String::class.java) ?: "" // Get display name
                                                    val userAvatar = document.child("avatar").getValue(String::class.java) ?: "" // Get user avatar
                                                    val userProfile = SearchUserData(userName, displayName, userID, userAvatar) // Create user data object
                                                    searchUserList.add(userProfile) // Add user object to results list
                                                }
                                                adapter.updateSearchState(true) // Notify adapter that results are available
                                            } else {
                                                adapter.updateSearchState(false) // Notify adapter of no results found
                                            }
                                            adapter.notifyDataSetChanged() // Update UI with search results
                                        }

//                                    db.collection("users")
//                                        .whereGreaterThanOrEqualTo("userName", query)
//                                        .whereLessThan("userName", upperBoundQuery)
//                                        .get()
//                                        .addOnCompleteListener { search ->
//                                            binding.progressBar.visibility = View.INVISIBLE // Hide loading indicator
//                                            searchUserList.clear() // Clear previous search results
//
//                                            if (search.isSuccessful && search.result.size() > 0) {
//                                                // Loop through results and add to the list if found
//                                                for (document in search.result) {
//                                                    val userID = document.id // Get user ID
//                                                    val userName = document.getString("userName") ?: "" // Get username
//                                                    val displayName = document.getString("displayname") ?: "" // Get display name
//                                                    val userAvatar = document.getString("avatar") ?: "" // Get user avatar
//                                                    val userProfile = SearchUserData(userName, displayName, userID, userAvatar) // Create user data object
//                                                    searchUserList.add(userProfile) // Add user object to results list
//                                                }
//                                                adapter.updateSearchState(true) // Notify adapter that results are available
//                                            } else {
//                                                adapter.updateSearchState(false) // Notify adapter of no results found
//                                            }
//                                            adapter.notifyDataSetChanged() // Update UI with search results
//                                        }
                                } else {
                                    // Clear results if query is empty after '@' and refresh adapter
                                    binding.progressBar.visibility = View.INVISIBLE // Hide loading indicator
                                    searchUserList.clear() // Clear results
                                    adapter.notifyDataSetChanged() // Refresh UI
                                }
                            } else {
                                // Handle local search when '@' is not used
                                binding.progressBar.visibility = View.INVISIBLE // Hide loading indicator
                                // Implementation for local search can be added here
                            }
                        }
                    }, delay) // Delay to manage excessive queries
                } else {
                    // Clear search results if the query is empty
                    searchUserList.clear() // Clear results list
                    adapter.updateSearchState(false) // Notify adapter of no results found
                    adapter.notifyDataSetChanged() // Refresh UI
                }
            }
        })
    }
}
