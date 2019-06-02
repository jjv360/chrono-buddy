package com.jjv360.chronobuddy.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView

class DeviceManagerActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1

    // UI
    var list : ListView? = null
    var devices = ArrayList<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create recycler view
        list = ListView(this)
//        list.setHasFixedSize(true)
//        list.layoutManager = LinearLayoutManager(this)
        setContentView(list)

//        list?.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values)

        // Get Bluetooth adapter. This should always succeed because we've specified that our app can only
        // be installed on devices with Bluetooth support in the manifest.
        val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return

        // Enable bluetooth if needed
        if (!bluetoothAdapter.isEnabled) {

            // Start bluetooth intent
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

        }

        // Find paired devices, add them to the device list
        devices.addAll(bluetoothAdapter.bondedDevices)
        refreshList()

    }

    // Updates the list view
    fun refreshList() {

        // Set adapter
        list?.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, devices.map { it.name })

    }

}
