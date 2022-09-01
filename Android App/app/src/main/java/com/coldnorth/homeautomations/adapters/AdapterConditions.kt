package com.coldnorth.homeautomations.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.coldnorth.homeautomations.Condition
import com.coldnorth.homeautomations.R
import com.coldnorth.homeautomations.Sensor

class AdapterConditions(val conditions: MutableList<Condition>, val sensors: MutableList<Sensor>) : RecyclerView.Adapter<AdapterConditions.MyViewHolder>() {


    override fun getItemCount(): Int {
        return conditions.size
    }

    fun clear() {
        conditions.clear()
        notifyDataSetChanged()
    }

    fun addItem(c:Condition){
        conditions.add(0,c)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.condition_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val condition = conditions[position]

        holder.valueEt.setText(condition.targetValue.toString())

        //dropdowns
        val ops = arrayListOf(">","==","<")
        val opsArrayAdapter= ArrayAdapter(holder.opDropdown.context, android.R.layout.simple_spinner_item, ops)
        opsArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.opDropdown.adapter = opsArrayAdapter
        holder.opDropdown.onItemSelectedListener=object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                condition.operator = ops[pos]
            }
        }
        holder.opDropdown.setSelection(ops.indexOf(condition.operator))

        val sensorNames = mutableListOf<String>()
        for(c in sensors){
            sensorNames.add("${c.name} - ${c.fatherName}")
        }

        val sensorsArrayAdapter= ArrayAdapter(holder.opDropdown.context, android.R.layout.simple_spinner_item, sensorNames)
        sensorsArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.sensorsDropdown.adapter = sensorsArrayAdapter
        holder.sensorsDropdown.onItemSelectedListener=object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                condition.deviceID = sensors[pos].id
            }
        }
        var selection = 0
        for(i in sensors.indices){
            if (sensors[i].id==condition.deviceID){
                selection = i
                break
            }
        }
        holder.sensorsDropdown.setSelection(selection)


        holder.valueEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (holder.valueEt.text.toString().isNotEmpty()) {
                    conditions[holder.adapterPosition].targetValue = holder.valueEt.text.toString().toInt()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.valueEt.setText(condition.targetValue.toString())

        holder.deleteBt.setOnClickListener {
            val pos = holder.adapterPosition
            conditions.removeAt(pos)
            notifyItemRemoved(pos)
        }


    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sensorsDropdown: Spinner = view.findViewById(R.id.sensor_dropdown)
        val opDropdown: Spinner = view.findViewById(R.id.operation_dropdown)
        val valueEt: EditText = view.findViewById(R.id.value_et)
        val deleteBt: ImageView = view.findViewById(R.id.delete_bt)
    }



}
