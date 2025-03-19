package com.example.chatease.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.example.chatease.R
import com.example.chatease.activities.Settings_AccountActivity
import com.example.chatease.activities.Settings_PrivacyActivity
import com.example.chatease.activities.UpdatePasswordActivity
import com.example.chatease.activities.WelcomeActivity
import com.example.chatease.adapters_listview.SettingsAdapter
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

        val toolbar = binding.activityToolbar // Setting up the toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar) // Setting the toolbar as the app bar
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)// Setting title for the toolbar

        val itemList = listOf(
            "Account",
            "Privacy & Security",
            "Update Password",
            "Logout"
        )
        val itemListMetaDataText = listOf(
            "Change Account Information",
            "Change Privacy Settings for Users",
            "Change Your Password",
            "Logout from the device"
        )
        val itemListIcons = listOf(
            R.drawable.vector_icon_account_settings,
            R.drawable.vector_icon_privacy,
            R.drawable.vector_icon_update_password,
            R.drawable.vector_icon_sign_out
        )
        binding.listView.adapter =
            SettingsAdapter(requireContext(), itemList, itemListMetaDataText, itemListIcons)

        binding.listView.setOnItemClickListener { parent, view, position, id ->

            when (position) {
                0 -> {
                    startActivity(Intent(requireContext(), Settings_AccountActivity::class.java))
                }

                1 -> {
                    startActivity(Intent(requireContext(), Settings_PrivacyActivity::class.java))
                }

                2 -> {
                    startActivity(Intent(requireContext(), UpdatePasswordActivity::class.java))
                }

                3 -> {
                    val alertDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Sign Out")
                        .setMessage("Do you want to Sign Out From the App?")
                        .setIcon(R.drawable.vector_icon_warning)
                        .setNegativeButton("No") { dialog, which ->
                            dialog.cancel()
                        }
                        .setPositiveButton("Yes") { dialog, which ->
                            rtDB.getReference("users").child(auth.currentUser!!.uid).updateChildren(
                                mapOf(
                                    "status" to "Offline",
                                    "lastHeartBeat" to ServerValue.TIMESTAMP
                                )
                            )
                            auth.signOut()
                            requireContext().getSharedPreferences(
                                "CurrentUserMetaData",
                                MODE_PRIVATE
                            ).edit().clear()
                                .apply()
                            val intent = Intent(
                                requireContext(),
                                WelcomeActivity::class.java
                            ).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                            val fragmentManager = parentFragmentManager
                            fragmentManager.beginTransaction()
                                .remove(this)
                                .commit()
                        }
                    alertDialog.show()
                }
            }

        }
    }


}
