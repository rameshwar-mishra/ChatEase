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
    val db = Firebase.firestore
    lateinit var recyclerView: RecyclerView
    private val handler = Handler(Looper.getMainLooper())
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
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView.layoutManager = LinearLayoutManager(this@SearchActivity)
        val searchUserList = mutableListOf<SearchUserData>()
        val adapter = SearchUserAdapter(searchUserList)
        recyclerView.adapter = adapter
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                var query = text.toString().trim()
                handler.removeCallbacksAndMessages(null)
                if (query.isNotEmpty()) {
                    handler.postDelayed({
                        if (binding.editTextSearch.text.toString().isNotEmpty()) {
                            binding.progressBar.visibility = View.VISIBLE

                            if (query.startsWith("@")) {
                                //if the search is Global which is indicated by using @
                                // e.g : @Goku
                                binding.progressBar.visibility = View.INVISIBLE
                                query = query.split("@")[1].trim()
                                val upperBoundQuery = query + "\uf8ff"
                                if(query.isNotEmpty()) {
                                    db.collection("users")
                                        .whereGreaterThanOrEqualTo("username", query)
                                        .whereLessThan("username", upperBoundQuery)
                                        .get()
                                        .addOnCompleteListener { search ->
                                            searchUserList.clear()

                                            if (search.isSuccessful && search.result.size() > 0) {
                                                for (document in search.result) {
                                                    val userID = document.id
                                                    val userName = document.getString("username") ?: ""
                                                    val userAvatar = document.getString("displayImage") ?: ""
                                                    val userProfile = SearchUserData(userName, userID, userAvatar)
                                                    searchUserList.add(userProfile)
                                                }
                                                adapter.updateSearchState(true)

                                            } else {
                                                adapter.updateSearchState(false)
                                            }
                                            adapter.notifyDataSetChanged()
                                        }
                                } else {
                                    searchUserList.clear()
                                    adapter.notifyDataSetChanged()
                                }
                            } else {
                                //if the search is Local which is indicated by not using @
                                // e.g : Goku
                                binding.progressBar.visibility = View.INVISIBLE

                            }
                        }
                    }, delay)
                } else {
                    searchUserList.clear()
                    adapter.updateSearchState(false)
                    adapter.notifyDataSetChanged()
                }
            }
        })

    }
}



