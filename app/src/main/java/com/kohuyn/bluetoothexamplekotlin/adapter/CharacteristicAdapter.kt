package com.kohuyn.bluetoothexamplekotlin.adapter

import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kohuyn.bluetoothexamplekotlin.R


/**
 * Created by KO Huyn on 1/14/2021.
 */
class CharacteristicAdapter :
    RecyclerView.Adapter<CharacteristicAdapter.CharacteristicViewHolder>() {

    var items = mutableListOf<Pair<String, String>>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class CharacteristicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtUUID = itemView.findViewById<TextView>(R.id.txtUUID)
        val txtProperty = itemView.findViewById<TextView>(R.id.txtProperty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacteristicViewHolder =
        CharacteristicViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_characteristic, null)
        )

    override fun onBindViewHolder(holder: CharacteristicViewHolder, position: Int) {
        holder.txtUUID.text = items[position].first
        holder.txtProperty.text = items[position].second
    }

    override fun getItemCount(): Int = items.size
}