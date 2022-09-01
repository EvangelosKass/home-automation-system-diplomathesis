package com.coldnorth.homeautomations

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.coldnorth.homeautomations.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }
}