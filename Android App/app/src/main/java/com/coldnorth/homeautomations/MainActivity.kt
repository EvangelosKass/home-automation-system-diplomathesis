package com.coldnorth.homeautomations

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.coldnorth.homeautomations.databinding.ActivityMainBinding
import com.coldnorth.homeautomations.fragments.AutomationsFragment
import com.coldnorth.homeautomations.fragments.HomeFragment
import com.google.android.material.tabs.TabLayoutMediator
import android.view.Menu
import com.coldnorth.homeautomations.utils.NetworkUtils


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        //init the MQTT connection
        NetworkUtils.connect()

        //set toolbar as actionbar
        setSupportActionBar(binding.toolbar)

        //init and set fragment adapter to viewpager
        val fragmentadapter = FragmentAdapter(this)
        binding.pager.adapter = fragmentadapter

        //connect tablayout with view pager
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            when(position) {
                0->tab.setIcon(R.drawable.ic_action_home)
                1->tab.setIcon(R.drawable.ic_action_category)
            }
        }.attach()


        setContentView(binding.root)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.action_add->{
            val intent = Intent(this, DiscoverDevicesActivity::class.java)
            startActivity(intent)
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }




    class FragmentAdapter(fragment: FragmentActivity): FragmentStateAdapter(fragment) {

        val tabNames = listOf(fragment.getString(R.string.home),fragment.getString(R.string.automations))

        override fun getItemCount(): Int {
            return tabNames.size
        }


        override fun createFragment(position: Int): Fragment {
            return when(position){
                1 -> AutomationsFragment(tabNames[position])
                else->{
                    HomeFragment(tabNames[position])
                }
            }

        }


    }





}