package com.example.chatease.adapters_listview

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.chatease.R
import com.example.chatease.databinding.LayoutSettingsListviewBinding

class SettingsAdapter(
    private val context: Context,
    private val itemList: List<String>,
    private val itemListMetaDataText : List<String>,
    private val itemListIcons : List<Int>
) : BaseAdapter() {

    override fun getCount(): Int = itemList.size

    override fun getItem(position: Int): String = itemList[position]

    override fun getItemId(position: Int): Long = position.toLong()
//Attaching Data to ListView Components
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = if(convertView == null){ //Using ViewBinding to access layouts Elements, convertView is recycled view from previous item of ListView which was scrolled out of area
            LayoutSettingsListviewBinding.inflate(LayoutInflater.from(context),parent,false).apply { //inflating the layout if convertView is null that means it is inflating for first time and storing it in the root.tag
                root.tag = this
            }

        }
        else{
            //if the convertView had something it will simply get typecast to ViewBinding and now we can set the data to it
          convertView.tag as LayoutSettingsListviewBinding
        }
        if(position == itemList.size -1){
            binding.views.visibility = View.GONE
            val color = ContextCompat.getColor(context, R.color.red)
            binding.textViewSettingsCategoryName.setTextColor(color)
            binding.imageViewForwardArrow.visibility = View.GONE
        }
        else{
            binding.views.visibility = View.VISIBLE
            val color = ContextCompat.getColor(context,R.color.textColors)
            binding.textViewSettingsCategoryName.setTextColor(color)
            binding.imageViewForwardArrow.visibility = View.VISIBLE
        }
        binding.textViewSettingsCategoryName.text = itemList[position]
        binding.textViewSettingsMetaData.text = itemListMetaDataText[position]
        binding.settingsCategoryIcon.setImageResource(itemListIcons[position])
        return binding.root
    }
}