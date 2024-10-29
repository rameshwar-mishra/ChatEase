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
    // Initialize Firestore database instance
    val db = Firebase.firestore
    lateinit var recyclerView: RecyclerView
    // Handler to delay search to improve performance and reduce redundant queries
    private val handler = Handler(Looper.getMainLooper())
    private val delay = 500L // 500 milliseconds delay for search
    lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup view binding
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable padding to avoid overlapping with system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Sets the custom toolbar
        recyclerView = findViewById(R.id.recyclerViewSearch)
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button on toolbar

        recyclerView.layoutManager = LinearLayoutManager(this@SearchActivity)
        val searchUserList = mutableListOf<SearchUserData>() // List to hold user search results
        val adapter = SearchUserAdapter(searchUserList)
        recyclerView.adapter = adapter

        // Add a text watcher to listen for changes in search input
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            // Handle input after the text has changed
            override fun afterTextChanged(text: Editable?) {
                var query = text.toString().trim() // Get search query, trimmed of leading/trailing spaces
                handler.removeCallbacksAndMessages(null) // Clear any previous search delay

                // Below a Delay/Timeout mechanism is implemented which is used to stop the spam API calls.

                // Workflow of the mechanism (also explains, Why we need it) : (Suppose a user types)

                // S                         (The Event Listener Invoked)
                // So                        (The Event Listener Invoked)
                // Sou                       (The Event Listener Invoked)
                // Souv                      (The Event Listener Invoked)
                // Souvi                     (The Event Listener Invoked)
                // Souvic                    (The Event Listener Invoked)
                // Souvick                   (The Event Listener Invoked)

                // As we can see here, it will lead to api spam calls as each time the user types
                // the event listener invokes, The search will happens.

                // To prevent this, a timeout mechanism is implemented.
                // When a user types a character the countDown starts. (Suppose its a 5 second countDown)
                // as the user types a character within 5 seconds. The countDown resets
                // if the countDown gets over then the query gets searched in the database


                if (query.isNotEmpty()) {
                    handler.postDelayed({
                        // Ensure search field still contains text before proceeding
                        if (binding.editTextSearch.text.toString().isNotEmpty()) {
                            binding.progressBar.visibility = View.VISIBLE // Show progress bar

                            if (query.startsWith("@")) {
                                // Global search (indicated by '@' prefix), for example: "@Goku"
                                binding.progressBar.visibility = View.INVISIBLE
                                query = query.split("@")[1].trim() // Remove '@' symbol from query
                                val upperBoundQuery = query + "\uf8ff" // Set upper bound for range query

                                if (query.isNotEmpty()) {
                                    // Perform Firestore query to search for users by username range
                                    db.collection("users")
                                        .whereGreaterThanOrEqualTo("username", query)
                                        .whereLessThan("username", upperBoundQuery)
                                        .get()
                                        .addOnCompleteListener { search ->
                                            searchUserList.clear() // Clear previous search results

                                            if (search.isSuccessful && search.result.size() > 0) {
                                                // Loop through results and add to list if found
                                                for (document in search.result) {
                                                    val userID = document.id
                                                    val userName = document.getString("username") ?: ""
                                                    val userAvatar = document.getString("displayImage") ?: ""
                                                    val userProfile = SearchUserData(userName, userID, userAvatar) // Creates a user object
                                                    searchUserList.add(userProfile) // adds the user object to the mutable list
                                                }
                                                adapter.updateSearchState(true) // Notify adapter that results are available
                                            } else {
                                                adapter.updateSearchState(false) // Notify adapter that no results were found
                                            }
                                            adapter.notifyDataSetChanged() // Update UI with results
                                        }
                                } else {
                                    // Clear list if query is empty after '@' and refresh adapter
                                    searchUserList.clear()
                                    adapter.notifyDataSetChanged()
                                }
                            } else {
                                // Local search (does not use '@' prefix)
                                binding.progressBar.visibility = View.INVISIBLE
                                // Implementation for local search can be added here
                            }
                        }
                    }, delay) // Delay search to avoid excessive queries
                } else {
                    // Clear search results if query is empty
                    searchUserList.clear()
                    adapter.updateSearchState(false) // Notify adapter that no results were found
                    adapter.notifyDataSetChanged() // Update UI
                }
            }
        })
    }
}
