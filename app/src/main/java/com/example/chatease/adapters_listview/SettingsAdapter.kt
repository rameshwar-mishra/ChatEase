package com.example.chatease.adapters_listview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.chatease.R
import com.example.chatease.databinding.LayoutSettingsListviewBinding

class SettingsRecyclerAdapter(
    private val context: Context,
    private val itemList: List<String>,
    private val itemListMetaDataText: List<String>,
    private val itemListIcons: List<Int>,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<SettingsRecyclerAdapter.SettingsViewHolder>() {

    inner class SettingsViewHolder(val binding: LayoutSettingsListviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.textViewSettingsCategoryName.text = itemList[position]
            binding.textViewSettingsMetaData.text = itemListMetaDataText[position]
            binding.settingsCategoryIcon.setImageResource(itemListIcons[position])

            // Special formatting for the "Logout" item


            val color = ContextCompat.getColor(context, R.color.textColors)
            binding.textViewSettingsCategoryName.setTextColor(color)
            binding.imageViewForwardArrow.visibility = View.VISIBLE


            binding.root.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val binding =
            LayoutSettingsListviewBinding.inflate(LayoutInflater.from(context), parent, false)
        return SettingsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = itemList.size
}
