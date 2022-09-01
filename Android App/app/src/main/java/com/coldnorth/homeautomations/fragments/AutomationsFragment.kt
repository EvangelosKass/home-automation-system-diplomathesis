package com.coldnorth.homeautomations.fragments


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.coldnorth.homeautomations.*
import com.coldnorth.homeautomations.adapters.AdapterAutomations
import com.coldnorth.homeautomations.databinding.FragmentAutomationsBinding
import com.coldnorth.homeautomations.utils.NetworkUtils
import org.json.JSONObject


class AutomationsFragment(private val title: String) : Fragment() {

    private var _binding:FragmentAutomationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var listadapter: AdapterAutomations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding= FragmentAutomationsBinding.inflate(inflater,container,false)

        listadapter = AdapterAutomations(mutableListOf())

        binding.automationsList.layoutManager = LinearLayoutManager(context)
        binding.automationsList.adapter = listadapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshAutomations()
        }

        binding.fab.setOnClickListener {
            val intent = Intent(requireContext(), AutomationActivity::class.java)
            startActivity(intent)
        }


        return binding.root
    }

    private fun refreshAutomations() {
        //ask server for automations list
        binding.swipeRefreshLayout.isRefreshing = true
        val data = JSONObject()
        data.put("action", "get_automations")
        NetworkUtils.publish(NetworkUtils.SERVER_TOPIC,data.toString())
    }

    private fun subscribe() {

        NetworkUtils.subscribe(NetworkUtils.APP_TOPIC) { publish ->
            //check topic
            if(publish.topic.toString() == NetworkUtils.APP_TOPIC){
                val datastr = Charsets.UTF_8.decode(publish.payload.get()).toString()
                val datajson = JSONObject(datastr)

                //get action to check after with if/else
                val action = datajson.get("action").toString()
                if (action == "automations_list"){
                    val automationsjson = datajson.getJSONArray("automations")
                    val conditionsjson = datajson.getJSONArray("conditions")
                    val automations = mutableListOf<Automation>()

                    //loop through conditions and refresh the automations list
                    for (i in 0 until automationsjson.length()) {
                        val a = automationsjson.getJSONObject(i)
                        val days = mutableListOf<Int>()
                        val dayArray=a["day_schedule"].toString().removeSurrounding("[","]").split(",").map { it }.toTypedArray()
                        for(d in dayArray){
                            days.add(d.replace(" ","").toInt())
                        }
                        val automation = Automation(a["id"] as Int,a["name"].toString(),a["target_device_id"].toString(),a["target_state"] as Int,
                            mutableListOf(), a["start_delay"].toString().toLong(),days,a["start_time"].toString(),a["end_time"].toString())
                        for (y in 0 until conditionsjson.length()) {
                            val cjson = conditionsjson.getJSONObject(y)
                            val condition = Condition(cjson["device_id"].toString(),cjson["op"].toString(),cjson["value"].toString().toInt(),cjson["father"].toString().toInt())
                            if (condition.father == automation.id){
                                automation.conditions.add(condition)
                            }
                        }
                        automations.add(automation)
                    }
                    binding.swipeRefreshLayout.post {
                        //refresh list
                        listadapter.clear()
                        listadapter.notifyDataSetChanged()
                        listadapter.addItems(automations)
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }

            }

        }


    }


    private fun unsubscribe(){
        NetworkUtils.unsubscribe(NetworkUtils.APP_TOPIC)
    }

    override fun onPause() {
        unsubscribe()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        subscribe()
        refreshAutomations()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = title
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}