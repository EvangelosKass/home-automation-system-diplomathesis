package com.coldnorth.homeautomations

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.coldnorth.homeautomations.databinding.ActivityAutomationBinding
import com.coldnorth.homeautomations.utils.NetworkUtils
import org.json.JSONObject
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.coldnorth.homeautomations.adapters.AdapterConditions
import com.coldnorth.homeautomations.utils.Helper
import org.json.JSONArray






class AutomationActivity : AppCompatActivity() {

    private var devices = mutableListOf<Cube>()
    private lateinit var binding: ActivityAutomationBinding

    val checkedDays = BooleanArray(7)

    private var conditionAdapter: AdapterConditions? = null

    private var timeStart:String=""
    private var timeEnd:String=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAutomationBinding.inflate(layoutInflater)
        checkedDays.fill(true) //check all days


        subscribe()
        refreshDevices()

        binding.selectDays.setOnClickListener {
            selectDaysDialog()
        }

        binding.addConditionBt.setOnClickListener {
            conditionAdapter?.addItem(Condition("","==",0))
        }

        binding.selectTime.setOnClickListener {
            val startPicker = Helper.getTimePicker("Start Time")
            val endPicker = Helper.getTimePicker("End Time")
            startPicker.addOnPositiveButtonClickListener {
                endPicker.show(supportFragmentManager,"endTime")
            }
            endPicker.addOnPositiveButtonClickListener {
                timeStart = "${startPicker.hour+startPicker.minute/100.0}"
                timeEnd ="${endPicker.hour+endPicker.minute/100.0}"
                binding.selectTime.text = "$timeStart - $timeEnd"
            }
            endPicker.addOnNegativeButtonClickListener{
                timeStart = "${startPicker.hour+startPicker.minute/100.0}"
                timeEnd =""
                binding.selectTime.text = timeStart
            }
            startPicker.show(supportFragmentManager,"startTime")
        }

        binding.saveBtn.setOnClickListener {
            val days = IntArray(7)
            for(i in checkedDays.indices){
                days[i]=checkedDays[i].compareTo(false) // returns 0 if false
            }
            if (binding.titleEt.text.toString().isEmpty()){ //if user does not provide a name
                binding.titleEt.setText("Unnamed")
            }
            val json = JSONObject()
            json.put("action","add_automation")
            json.put("name",binding.titleEt.text.toString())
            json.put("target_id",binding.cubelistDropdown.selectedItem.toString().split(" - ")[1]) // dropdown format is "cube_name - cube_id". we only need the id
            json.put("target_state",binding.targetState.isChecked.compareTo(false)) // 0 if false
            json.put("start_delay",binding.delayEt.text.toString().toInt())
            json.put("days",JSONArray(days))
            json.put("start_time",timeStart)
            json.put("end_time",timeEnd)
            val conditionArray = JSONArray()
            for (c in conditionAdapter!!.conditions){
                val jsonc = JSONObject()
                jsonc.put("device_id",c.deviceID)
                jsonc.put("op",c.operator)
                jsonc.put("t_value",c.targetValue)
                conditionArray.put(jsonc)
            }
            json.put("conditions",conditionArray)

            NetworkUtils.publish(NetworkUtils.SERVER_TOPIC,json.toString())
            finish()
        }
        binding.deleteBtn.setOnClickListener {
            finish()
        }

        setContentView(binding.root)
    }



    private fun refreshDevices(){
        //ask server for device list
        val data = JSONObject()
        data.put("action", "get_devices")
        NetworkUtils.publish(NetworkUtils.SERVER_TOPIC,data.toString())
    }

    private fun subscribe(){

        NetworkUtils.subscribe(NetworkUtils.APP_TOPIC) { publish ->
            //check topic
            if(publish.topic.toString() == NetworkUtils.APP_TOPIC){
                val datastr = Charsets.UTF_8.decode(publish.payload.get()).toString()
                val datajson = JSONObject(datastr)

                //get action to check after with if/else
                val action = datajson.get("action").toString()

                if (action == "device_list"){
                    val cubesjson = datajson.getJSONArray("cubes")
                    val sensorsjson = datajson.getJSONArray("sensors")
                    devices = mutableListOf()
                    val cubeNames = mutableListOf<String>()
                    val sensors = mutableListOf<Sensor>()

                    //loop through devices and refresh the list
                    for (i in 0 until cubesjson.length()) {
                        val c = cubesjson.getJSONObject(i)
                        val cube = Cube(c["id"].toString(),c["name"].toString(),c["status"].toString().toInt(),mutableListOf())
                        cubeNames.add("${cube.name} - ${cube.id}")
                        for (y in 0 until sensorsjson.length()) {
                            val s = sensorsjson.getJSONObject(y)
                            if (s["father"].toString()==cube.id){
                                val sensor = Sensor(s["id"].toString(),s["name"].toString(),s["status"].toString(),s["father"].toString(),cube.name)
                                cube.children.add(sensor)
                                sensors.add(sensor)
                            }
                        }
                        devices.add(cube)
                    }
                    unsubscribe()
                    binding.cubelistDropdown.post {
                        //populate dropdown with cubename - id
                        val spinnerArrayAdapter= ArrayAdapter(this, android.R.layout.simple_spinner_item, cubeNames)
                        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.cubelistDropdown.adapter = spinnerArrayAdapter

                        conditionAdapter = AdapterConditions(mutableListOf(),sensors)
                        binding.conditionList.layoutManager = LinearLayoutManager(this)
                        binding.conditionList.adapter = conditionAdapter

                    }
                }


            }

        }
    }

    private fun selectDaysDialog() {

        val builder = AlertDialog.Builder(this)
        val daysList = resources.getStringArray(R.array.days_array)
        val checkedDaystmp = checkedDays.copyOf()

        builder.setMultiChoiceItems(daysList, checkedDaystmp) { dialogInterface: DialogInterface, i: Int, b: Boolean ->
            // Update the current focused item's checked status
            checkedDaystmp[i] = b
        }

        builder.setPositiveButton(android.R.string.ok){dialogInterface: DialogInterface, i: Int ->
            //user pressed ok -> update the checked days
            var days_str = ""
            for(index in checkedDaystmp.indices){
                checkedDays[index]=checkedDaystmp[index]
                if (checkedDays[index]){
                    days_str+=resources.getStringArray(R.array.days_array)[index]+" "
                }
            }
            binding.selectDays.text = days_str
        }
        builder.setNegativeButton(android.R.string.cancel){dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        builder.create().show()

    }


    private fun unsubscribe(){
        NetworkUtils.unsubscribe(NetworkUtils.APP_TOPIC)
    }

    override fun onDestroy() {
        unsubscribe()
        super.onDestroy()
    }


}