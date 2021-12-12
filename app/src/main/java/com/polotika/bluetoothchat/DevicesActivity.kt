package com.polotika.bluetoothchat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.polotika.bluetoothchat.databinding.ActivityDevicesBinding


class DevicesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDevicesBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var availableDevicesAdapter: ArrayAdapter<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        availableDevicesAdapter = ArrayAdapter<String>(this, R.layout.device_item)
        val pairedDevicesAdapter = ArrayAdapter<String>(this, R.layout.device_item)
        binding.listAvailableDevices.adapter = availableDevicesAdapter
        binding.listPairedDevices.adapter = pairedDevicesAdapter

        binding.listAvailableDevices.onItemClickListener =
            OnItemClickListener { _, view, _, _ ->
                val info = (view as TextView).text.toString()
                val address = info.substring(info.length - 17)
                val intent = Intent()
                intent.putExtra("deviceAddress", address)
                setResult(RESULT_OK, intent)
                finish()
            }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                pairedDevicesAdapter.add(device.name + "\n" + device.address)
            }
        }

        val deviceFoundIntentFilter: IntentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothDevicesListener, deviceFoundIntentFilter)

        val discoverFinishedIntentFilter: IntentFilter =
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(bluetoothDevicesListener, discoverFinishedIntentFilter)

        binding.listPairedDevices.onItemClickListener =
            OnItemClickListener { _, view, _, _ ->
                bluetoothAdapter.cancelDiscovery()
                val info = (view as TextView).text.toString()
                val address = info.substring(info.length - 17)
                Log.d("Address", address)
                val intent = Intent()
                intent.putExtra("deviceAddress", address)
                setResult(RESULT_OK, intent)
                finish()
            }
    }

    private val bluetoothDevicesListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val bluetoothDevice: BluetoothDevice =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                if (bluetoothDevice.bondState != BluetoothDevice.BOND_BONDED) {
                    availableDevicesAdapter?.add(bluetoothDevice.name + "\n" + bluetoothDevice.address)
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                binding.progressBar.visibility = GONE
                if (availableDevicesAdapter?.isEmpty == true)
                    Snackbar.make(binding.root, "No available devices found", Snackbar.LENGTH_SHORT)
                        .show()
                else
                    Snackbar.make(binding.root, "Click the device to chat", Snackbar.LENGTH_SHORT)
                        .show()


            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.devices_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_search -> {
                scanDevices()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scanDevices() {
        binding.progressBar.visibility = VISIBLE
        availableDevicesAdapter?.clear()
        Toast.makeText(this, "Scan started", Toast.LENGTH_SHORT).show()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.startDiscovery()


    }


    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothDevicesListener != null) {
            unregisterReceiver(bluetoothDevicesListener)
        }
    }
}