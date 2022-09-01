package com.coldnorth.homeautomations.adapters


import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.coldnorth.homeautomations.Automation
import com.coldnorth.homeautomations.R
import com.coldnorth.homeautomations.utils.Helper
import com.coldnorth.homeautomations.utils.NetworkUtils
import org.json.JSONObject

class AdapterAutomations(private val automations: MutableList<Automation>) : RecyclerView.Adapter<AdapterAutomations.MyViewHolder>() {


    override fun getItemCount(): Int {
        return automations.size
    }

    fun clear() {
        automations.clear()
        notifyDataSetChanged()
    }

    fun addItems(c:MutableList<Automation>){
        automations.addAll(c)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.automation_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val automation = automations[position]

        holder.titletv.text = automation.name

        //remove automation
        holder.deletebt.setOnClickListener {
            val context = holder.deletebt.context
            Helper.showDialogYesNo(context,context.getString(R.string.confirm_title),context.getString(R.string.confirm_delete),{ dialogInterface: DialogInterface, i: Int ->
                val json = JSONObject()
                json.put("action","remove_automation")
                json.put("id",automation.id)
                NetworkUtils.publish(NetworkUtils.SERVER_TOPIC,json.toString())
            },{ dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss()
            })
        }


    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titletv: TextView = view.findViewById(R.id.title)
        val deletebt: ImageView = view.findViewById(R.id.delete_bt)
    }



}