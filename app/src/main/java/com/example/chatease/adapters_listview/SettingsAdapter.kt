package com.example.chatease.adapters_listview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.chatease.R

class SettingsAdapter(
    private val context: Context,
    private val itemList: List<String>
) : BaseAdapter() {

    override fun getCount(): Int = itemList.size

    override fun getItem(position: Int): String = itemList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.layout_settings_listview, parent, false)

        val textView = view.findViewById<TextView>(R.id.textView)
        textView.text = itemList[position]
        return view
    }
}