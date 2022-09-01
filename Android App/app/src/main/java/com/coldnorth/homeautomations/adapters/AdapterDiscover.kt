package com.coldnorth.homeautomations.adapters

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.coldnorth.homeautomations.R

class AdapterDiscover(
    private val cubes: MutableList<BluetoothDevice>, private val btadapter: BluetoothAdapter?,private val spinner: ProgressBar) : RecyclerView.Adapter<AdapterDiscover.MyViewHolder>() {


    override fun getItemCount(): Int {
        return cubes.size
    }

    fun addItem(c: BluetoothDevice){
        cubes.add(0,c)
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.device_discover_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val cube = cubes[position]

        holder.titletv.text = cube.name


        holder.card.setOnClickListener {
            btadapter?.cancelDiscovery()
            spinner.visibility = View.GONE
            cube.createBond()
        }


    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titletv: TextView = view.findViewById(R.id.title)
        val card:CardView = view.findViewById(R.id.card_view)
    }



}