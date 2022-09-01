package com.coldnorth.homeautomations.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.coldnorth.homeautomations.Cube
import com.coldnorth.homeautomations.Sensor
import com.coldnorth.homeautomations.adapters.AdapterHome
import com.coldnorth.homeautomations.databinding.FragmentHomeBinding
import com.coldnorth.homeautomations.utils.NetworkUtils
import org.json.JSONObject

import kotlin.text.Charsets.UTF_8
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import androidx.recyclerview.widget.RecyclerView





class HomeFragment(private val title: String) : Fragment() {


    private var _binding:FragmentHomeBinding?=null
    private val binding get() = _binding!!



    private lateinit var listadapter: AdapterHome


    private var devices = mutableListOf<Cube>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater,container,false)


        listadapter = AdapterHome(devices)

        binding.deviceList.layoutManager = LinearLayoutManager(context)
        binding.deviceList.adapter = listadapter



        //add swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshDevices()
        }

        //Ability to rearrange devices
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    if (fromPos==toPos){
                        return false
                    }
                    listadapter.notifyItemMoved(fromPos,toPos)
                    return true // true if moved, false otherwise
                }

                override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}
            })
        itemTouchHelper.attachToRecyclerView(binding.deviceList)

        return binding.root
    }

    private fun refreshDevices(){
        binding.swipeRefreshLayout.isRefreshing = true
        //ask server for device list
        val data = JSONObject()
        data.put("action", "get_devices")
        NetworkUtils.publish(NetworkUtils.SERVER_TOPIC,data.toString())
    }

    private fun subscribe(){

        NetworkUtils.subscribe(NetworkUtils.APP_TOPIC) { publish ->
            //check topic
            if(publish.topic.toString() == NetworkUtils.APP_TOPIC){
                val datastr = UTF_8.decode(publish.payload.get()).toString()
                val datajson = JSONObject(datastr)

                //get action to check after with if/else
                val action = datajson.get("action").toString()

                if (action == "device_list"){
                    val cubesjson = datajson.getJSONArray("cubes")
                    val sensorsjson = datajson.getJSONArray("sensors")
                    devices = mutableListOf()

                    //loop through devices and refresh the list
                    for (i in 0 until cubesjson.length()) {
                        val c = cubesjson.getJSONObject(i)
                        val cube = Cube(c["id"].toString(),c["name"].toString(),c["status"].toString().toInt(),mutableListOf())
                        for (y in 0 until sensorsjson.length()) {
                            val s = sensorsjson.getJSONObject(y)
                            if (s["father"].toString()==cube.id){
                                val sensor = Sensor(s["id"].toString(),s["name"].toString(),s["status"].toString(),s["father"].toString(),cube.name)
                                cube.children.add(sensor)
                            }
                        }
                        devices.add(cube)
                    }
                    binding.deviceList.post {
                        listadapter.clear()
                        listadapter.notifyDataSetChanged()
                        listadapter.addItems(devices)
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }else if(action == "device_status_update"){
                    var cubetoUpdate:Cube? = null
                    val id = datajson.get("id").toString()
                    val status = datajson.get("status")
                    //search cubes
                    for (d in devices){
                        if (d.id == id){
                            d.status = status.toString().toInt()
                            cubetoUpdate =d
                            break;
                        }
                        //search children
                        for (c in d.children){
                            if (c.id == id){
                                c.status = status.toString()
                                cubetoUpdate =d
                                break
                            }
                        }
                    }
                    //update the ui, if cube is found
                    if (cubetoUpdate!=null) {
                        binding.deviceList.post {
                            binding.deviceList.adapter?.notifyItemChanged(devices.indexOf(cubetoUpdate))
                        }
                    }
                }


            }

        }
    }
    private fun unsubscribe(){
        NetworkUtils.unsubscribe(NetworkUtils.APP_TOPIC)
    }


    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = title
        subscribe()
        refreshDevices()
    }
    override fun onPause() {
        unsubscribe()
        super.onPause()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        unsubscribe()
        _binding = null
    }


}