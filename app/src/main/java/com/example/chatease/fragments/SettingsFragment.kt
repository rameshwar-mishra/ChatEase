package com.example.chatease.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatease.R
import com.example.chatease.activities.Settings_AccountActivity
import com.example.chatease.activities.Settings_PrivacyActivity
import com.example.chatease.activities.UpdatePasswordActivity
import com.example.chatease.activities.WelcomeActivity
import com.example.chatease.adapters_listview.SettingsRecyclerAdapter
import com.example.chatease.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class SettingsFragment : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val rtDB = FirebaseDatabase.getInstance()
    lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = binding.activityToolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        val itemList = listOf(
            "Account",
            "Privacy & Security",
            "Update Password",
            "Logout"
        )
        val itemListMetaDataText = listOf(
            "Change account information",
            "Change privacy settings for users",
            "Change your password",
            "Logout from the device"
        )
        val itemListIcons = listOf(
            R.drawable.vector_icon_account_settings,
            R.drawable.vector_icon_privacy,
            R.drawable.vector_icon_update_password,
            R.drawable.vector_icon_sign_out
        )

        binding.listView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SettingsRecyclerAdapter(
                requireContext(),
                itemList,
                itemListMetaDataText,
                itemListIcons
            ) { position ->
                when (position) {
                    0 -> startActivity(Intent(requireContext(), Settings_AccountActivity::class.java))
                    1 -> startActivity(Intent(requireContext(), Settings_PrivacyActivity::class.java))
                    2 -> startActivity(Intent(requireContext(), UpdatePasswordActivity::class.java))
                    3 -> showLogoutDialog()
                }
            }
        }
    }

    private fun showLogoutDialog() {
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log Out")
            .setMessage("Do you want to Log Out From the App?")
            .setIcon(R.drawable.vector_icon_warning)
            .setNegativeButton("No") { dialog, _ -> dialog.cancel() }
            .setPositiveButton("Yes") { _, _ ->
                rtDB.getReference("users").child(auth.currentUser!!.uid).updateChildren(
                    mapOf(
                        "status" to "Offline",
                        "lastHeartBeat" to ServerValue.TIMESTAMP
                    )
                )
                auth.signOut()
                requireContext().getSharedPreferences("CurrentUserMetaData", MODE_PRIVATE)
                    .edit().clear().apply()
                val intent = Intent(requireContext(), WelcomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                parentFragmentManager.beginTransaction().remove(this).commit()
            }
        alertDialog.show()
    }
    }

