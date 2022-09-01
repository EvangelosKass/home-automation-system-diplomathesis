package com.coldnorth.homeautomations.utils

import android.content.Context
import androidx.preference.PreferenceManager


object SaveManager {

    private const val WIFISSID_FLAG = "wifi_ssid"
    private const val WIFIPASSWORD_FLAG = "wifi_password"


    fun getWifiSSID(c: Context):String{
        return PreferenceManager.getDefaultSharedPreferences(c).getString(WIFISSID_FLAG,"")!!
    }

    fun setWifiSSID(c:Context,ssid:String){
        PreferenceManager.getDefaultSharedPreferences(c).edit().putString(WIFISSID_FLAG,ssid).apply()
    }

    fun getWifiPass(c: Context):String{
        return PreferenceManager.getDefaultSharedPreferences(c).getString(WIFIPASSWORD_FLAG,"")!!
    }

    fun setWifiPass(c:Context,pass:String){
        PreferenceManager.getDefaultSharedPreferences(c).edit().putString(WIFIPASSWORD_FLAG,pass).apply()
    }

}