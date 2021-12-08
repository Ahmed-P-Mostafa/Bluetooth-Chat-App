package com.polotika.bluetoothchat

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.polotika.bluetoothchat.ChatUtils.States.STATE_CONNECTED
import com.polotika.bluetoothchat.ChatUtils.States.STATE_CONNECTING
import com.polotika.bluetoothchat.ChatUtils.States.STATE_LISTEN
import com.polotika.bluetoothchat.ChatUtils.States.STATE_NONE
import com.polotika.bluetoothchat.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: MainActivityVIewModel
    private lateinit var connectedDevice: String
    private lateinit var adapterMainChat: ArrayAdapter<String>
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var chatUtils: ChatUtils


    private val handler: Handler = Handler { message ->
        when (message.what) {
            MESSAGE_STATE_CHANGED ->  {
                when (message.arg1) {
                    STATE_NONE -> setState("Not Connected")
                    STATE_LISTEN -> setState("Not Connected")
                    STATE_CONNECTING -> setState("Connecting...")
                    STATE_CONNECTED -> setState("Connected: $connectedDevice")
                }
            }
            MESSAGE_WRITE -> {
                val buffer1 = message.obj as ByteArray
                val outputBuffer = String(buffer1)
                adapterMainChat.add("Me: $outputBuffer")
            }
            MESSAGE_READ -> {
                val buffer = message.obj as ByteArray
                val inputBuffer = String(buffer, 0, message.arg1)
                adapterMainChat.add(connectedDevice.toString() + ": " + inputBuffer)
            }
            MESSAGE_DEVICE_NAME -> {
                connectedDevice = message.getData().getString(DEVICE_NAME)!!
                Toast.makeText(this, connectedDevice, Toast.LENGTH_SHORT).show()
            }
            MESSAGE_TOAST -> Toast.makeText(
                this,
                message.getData().getString(TOAST),
                Toast.LENGTH_SHORT
            ).show()
        }
        false
    }

    private fun setState(subTitle: CharSequence) {
        supportActionBar!!.subtitle = subTitle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()

        homeViewModel =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
                MainActivityVIewModel::class.java
            )
        homeViewModel.init(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_devices -> {
                if (!isLocationPermissionGranted())
                    requestLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                else startDevicesActivity()
            }
            R.id.menu_item_enable_bluetooth -> {
                enableBluetooth()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            val address = data?.getStringExtra("deviceAddress")
            Log.d(TAG, "onActivityResult: $address")
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun init() {
        adapterMainChat = ArrayAdapter(this,R.layout.device_item)
        binding.conversationRv.adapter = adapterMainChat
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        chatUtils = ChatUtils(context = this, handler = handler)
        binding.sendBtn.setOnClickListener {
            val message = binding.textInputTv.text.toString()
            if (message.isNotBlank()){
                binding.textInputTv.setText("")
                chatUtils.write(message.toByteArray())
            }
        }

    }

    private fun enableBluetooth() {
        if (bluetoothAdapter == null) {
            Snackbar.make(binding.root, "Device don't have bluetooth", Snackbar.LENGTH_SHORT).show()
        } else {
            if (!bluetoothAdapter.isEnabled) bluetoothAdapter.enable()
            if (bluetoothAdapter.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                val discoverIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                startActivity(discoverIntent)
            }
        }
    }

    private fun showLocationPermissionRationale(log: String) {
        Log.d(TAG, "showLocationPermissionRationale: $log")
        AlertDialog.Builder(this)
            .setMessage("Bluetooth chat app need the location permission to find the devices near you\nPlease Grant")
            .setTitle("Location Permission Required")
            .setPositiveButton("Grant") { i, _ ->
                i.dismiss()
                requestLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }.setNegativeButton("Deny") { _, _ ->
                finish()
            }.show()
    }

    private fun startDevicesActivity() {
        val intent = Intent(this, DevicesActivity::class.java)
        startActivityForResult(intent, SELECT_DEVICE)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }



    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setMessage("Location Permission is denied so it needs to enabled manually\nPlease enable")
            .setTitle("Location Permission Required")
            .setPositiveButton("Grant") { i, _ ->
                i.dismiss()
                val settingIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(settingIntent)

            }.setNegativeButton("Deny") { _, _ ->
                finish()
            }.show()
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startDevicesActivity()
            } else {
                when {
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                        showLocationPermissionRationale("request rationale")
                    }
                    ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PERMISSION_DENIED -> {
                        showPermissionDeniedDialog()
                    }
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        if (chatUtils != null) {
            chatUtils.stop()
        }
    }

    companion object {
        const val MESSAGE_STATE_CHANGED = 0
        const val MESSAGE_READ = 1
        const val MESSAGE_WRITE = 2
        const val MESSAGE_DEVICE_NAME = 3
        const val MESSAGE_TOAST = 4


        const val DEVICE_NAME = "deviceName"
        const val TOAST = "toast"

        private const val LOCATION_PERMISSION_REQUEST = 101
        private const val SELECT_DEVICE = 102

    }

}