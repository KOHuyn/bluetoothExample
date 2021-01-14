package com.kohuyn.bluetoothexamplekotlin.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kohuyn.bluetoothexamplekotlin.R


/**
 * Created by KO Huyn on 1/14/2021.
 */
class ScanAdapter : RecyclerView.Adapter<ScanAdapter.ScanViewHolder>() {

    var items = mutableListOf<BluetoothDevice>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onClickDevice: OnClickDeviceBleListener? = null

    fun addDeviceBluetooth(device: BluetoothDevice) {
        items.withIndex()
            .firstOrNull { it.value.address == device.address }?.let {
                items[it.index] == device
                notifyItemChanged(it.index)
            } ?: run {
            with(items) {
                add(device)
                sortBy { it.address }
                notifyDataSetChanged()
            }
        }
    }

    inner class ScanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bleName = itemView.findViewById<TextView>(R.id.txtBleName)
        val bleAddress = itemView.findViewById<TextView>(R.id.txtBleAddress)
        val bleStatus = itemView.findViewById<TextView>(R.id.txtBleStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder =
        ScanViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_scan, null))

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        val item = items[position]
        with(holder) {
            bleName.text = item.name ?: "NoName"
            bleAddress.text = item.address
        }
        holder.itemView.setOnClickListener {
            onClickDevice?.setOnClickDevice(
                holder.itemView,
                position
            )
        }
    }

    override fun getItemCount(): Int = items.size

    fun interface OnClickDeviceBleListener {
        fun setOnClickDevice(view: View, pos: Int)
    }

}