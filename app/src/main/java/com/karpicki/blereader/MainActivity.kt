package com.karpicki.blereader

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import java.util.*

class MainActivity : AppCompatActivity() {

    private var isBackgroundServiceRunning = false

    private fun start() {

//        if (isBackgroundServiceRunning) {
//            return
//        }

        val bleServiceIntent = Intent(applicationContext, BLEBackgroundService::class.java)
        val thingSpeakIntent = Intent(applicationContext, ThingSpeakBackgroundService::class.java)

        startService(bleServiceIntent)
        startService(thingSpeakIntent)

        isBackgroundServiceRunning = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            //putSerializable(Constants.listName, messageList)
            putBoolean(Constants.semaphoreName, isBackgroundServiceRunning)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) { // Here You have to restore count value
        super.onRestoreInstanceState(savedInstanceState)

        //messageList = savedInstanceState.getSerializable(Constants.listName) as ArrayList<Message>
        isBackgroundServiceRunning = savedInstanceState.getBoolean(Constants.semaphoreName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializes Bluetooth adapter.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val requestEnableBT = 1
            startActivityForResult(enableBtIntent, requestEnableBT)
        } else {
            val button: Button = findViewById(R.id.button)
            button.setOnClickListener { start() }

            start()
        }

    }
}