package com.coldnorth.homeautomations.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.coldnorth.homeautomations.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}