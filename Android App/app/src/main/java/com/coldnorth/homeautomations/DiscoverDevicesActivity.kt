package com.coldnorth.homeautomations


import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.coldnorth.homeautomations.adapters.AdapterDiscover
import com.coldnorth.homeautomations.databinding.ActivityDiscoverDevicesBinding
import com.coldnorth.homeautomations.utils.SaveManager
import org.json.JSONObject
import java.lang.Exception
import java.util.*


class DiscoverDevicesActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val BLUETOOTH_CONNECT_REQUEST_CODE = 2
    private val UUID_BT_DEVICE: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


    private lateinit var binding:ActivityDiscoverDevicesBinding

    private var btadapter: 	BluetoothAdapter?=null

    private lateinit var listadapter: AdapterDiscover


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoverDevicesBinding.inflate(layoutInflater)


        if(SaveManager.getWifiPass(this).isEmpty() && SaveManager.getWifiSSID(this).isEmpty()){
            Toast.makeText(this,"Set wifi info on settings",Toast.LENGTH_LONG).show()
        }


        //request location permission in order to discover bluetooth devices
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED  ){
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTH_CONNECT_REQUEST_CODE)
            }
        }else{
            // Register for broadcasts when a device is discovered.
            btadapter=(getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

            val filter = IntentFilter()
            filter.addAction(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            registerReceiver(receiver, filter)
            btadapter?.startDiscovery()

            //init adapter with empty list
            listadapter = AdapterDiscover(mutableListOf(),btadapter,binding.spinner)
            binding.deviceList.adapter = listadapter

        }

        setContentView(binding.root)

    }


    // Create a BroadcastReceiver for ACTION_FOUND and ACTION_BOND_STATE_CHANGED
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                // Discovery has found a device
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    if (deviceName!!.startsWith("Cube")){
                        listadapter.addItem(device)
                    }
                }
                //Bonded with a device. Connect and send wifi creds
                BluetoothDevice.ACTION_BOND_STATE_CHANGED->{
                    val bondstate: Int? = intent.extras?.getInt(BluetoothDevice.EXTRA_BOND_STATE)
                    if (bondstate == BluetoothDevice.BOND_BONDED){
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val json = JSONObject()
                        json.put("ssid",SaveManager.getWifiSSID(this@DiscoverDevicesActivity))
                        json.put("pwd",SaveManager.getWifiPass(this@DiscoverDevicesActivity))
                        val socket = device?.createRfcommSocketToServiceRecord(UUID_BT_DEVICE)
                        socket?.connect()
                        val outputStream = socket?.outputStream
                        outputStream?.write(json.toString().toByteArray())
                        socket?.close()
                        finishAffinity()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //On destroy releash the reciever.
        try{
            unregisterReceiver(receiver)
        }catch (e:Exception){ }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE && requestCode!=BLUETOOTH_CONNECT_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recreate()
        } else {
            // Permission was denied. exit
            finishAffinity()
        }
    }

}