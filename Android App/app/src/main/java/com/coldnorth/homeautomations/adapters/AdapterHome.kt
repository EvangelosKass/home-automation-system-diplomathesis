package com.coldnorth.homeautomations.adapters

import android.content.DialogInterface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.coldnorth.homeautomations.Cube
import com.coldnorth.homeautomations.R
import com.coldnorth.homeautomations.utils.Helper
import com.coldnorth.homeautomations.utils.NetworkUtils
import org.json.JSONObject

class AdapterHome(private val cubes: MutableList<Cube>) : RecyclerView.Adapter<AdapterHome.MyViewHolder>() {


    override fun getItemCount(): Int {
        return cubes.size
    }

    fun clear() {
        cubes.clear()
        notifyDataSetChanged()
    }

    fun addItems(c:MutableList<Cube>){
        cubes.addAll(c)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.device_home_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val cube = cubes[position]


        //add sensors
        holder.childrenlayout.removeAllViews()
        for (s in cube.children){
            val tv = TextView(holder.childrenlayout.context)
            tv.text = "${s.name}: ${s.status}"
            holder.childrenlayout.addView(tv)
        }

        holder.titletv.text = cube.name
        if (cube.status!=0){
            holder.onoffswitchtv.setBackgroundResource(R.drawable.button_on)
            holder.onoffswitchtv.text = "On"
        }else{
            holder.onoffswitchtv.setBackgroundResource(R.drawable.button_off)
            holder.onoffswitchtv.text = "Off"
        }
        holder.onoffswitchtv.isEnabled = true
        holder.onoffswitchtv.alpha = 1f
        holder.onoffswitchtv.setOnClickListener {
            val json = JSONObject()
            json.put("action","trigger_device")
            json.put("id",cube.id)
            json.put("trigger",if(cube.status!=0)0 else 1)
            NetworkUtils.publish(NetworkUtils.DEVICE_TOPIC,json.toString())
            holder.onoffswitchtv.isEnabled = false
            holder.onoffswitchtv.alpha = 0.3f
        }

        //remove device
        holder.deletebtn.setOnClickListener {
            val context = holder.deletebtn.context
            Helper.showDialogYesNo(context,context.getString(R.string.confirm_title),context.getString(R.string.confirm_delete),{ dialogInterface: DialogInterface, i: Int ->
                val json = JSONObject()
                json.put("action","remove_device")
                json.put("id",cube.id)
                NetworkUtils.publish(NetworkUtils.DEVICE_TOPIC,json.toString())
            },{ dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss()
            })
        }

        //rename device
        holder.renamebtn.setOnClickListener {
            val context = holder.renamebtn.context
            val builder = androidx.appcompat.app.AlertDialog.Builder(context)
            builder.setTitle(cube.name)
            // Set up the input
            val input = EditText(context)
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.isSingleLine = true
            input.setText(cube.name)
            input.gravity = Gravity.CENTER

            builder.setView(input)
            // Set up the buttons
            builder.setPositiveButton(android.R.string.ok){ dialogInterface: DialogInterface, i: Int ->
                val json = JSONObject()
                json.put("action", "rename_device")
                json.put("id", cube.id)
                json.put("new_name", input.text.toString())
                NetworkUtils.publish(NetworkUtils.SERVER_TOPIC, json.toString())
                cube.name = input.text.toString()
                notifyItemChanged(position)
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, which -> dialog.cancel() }
            builder.create().show()

        }



    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titletv: TextView = view.findViewById(R.id.title)
        val childrenlayout: LinearLayout = view.findViewById(R.id.childer_layout)
        val onoffswitchtv: TextView = view.findViewById(R.id.onoff_switch)
        val deletebtn: ImageView = view.findViewById(R.id.delete_btn)
        val renamebtn: ImageView = view.findViewById(R.id.rename_btn)
    }



}