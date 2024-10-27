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
import com.example.chatease.R
import com.example.chatease.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val delay = 500L
    lateinit var binding: ActivitySearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.editTextSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(text: Editable?) {
                var query = text.toString()
                handler.removeCallbacksAndMessages(null)
                if(query.isNotEmpty()) {
                    handler.postDelayed({
                        if(binding.editTextSearch.text.toString().isNotEmpty()){
                            binding.progressBar.visibility = View.VISIBLE
                            Log.d("text",query)
                            if(query.startsWith("@"))
                            {
                                //if the search is Global which is indicated by using @
                                // e.g : @Goku
                                query = query.split("@")[1]
                                Log.d("text", "Global $query")
                            } else {
                                //if the search is Local which is indicated by not using @
                                // e.g : Goku
                                Log.d("text", "Local $query")
                            }
                        }
                    },delay)
                }
            }
        })
    }
}



