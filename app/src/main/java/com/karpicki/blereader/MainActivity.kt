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

    private val propListName = "MESSAGE_LIST"
    private val propSemaphoreName = "IS_RUNNING"
    private var isBackgroundServiceRunning = false
    private var messageList = ArrayList<Message>()

    private fun start() {

// @todo - uncomment me at some point :)
//        if (isBackgroundServiceRunning) {
//            return
//        }

        val queue = PriorityQueue<Message>()
        val bleServiceIntent = Intent(applicationContext, BLEBackgroundService::class.java)
        val thingSpeakIntent = Intent(applicationContext, ThingSpeakBackgroundService::class.java)

        bleServiceIntent.putExtra(propListName, queue)
        thingSpeakIntent.putExtra(propListName, queue)

        startService(bleServiceIntent)
        startService(thingSpeakIntent)

        isBackgroundServiceRunning = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putSerializable(propListName, messageList)
            putBoolean(propSemaphoreName, isBackgroundServiceRunning)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) { // Here You have to restore count value
        super.onRestoreInstanceState(savedInstanceState)

        messageList = savedInstanceState.getSerializable(propListName) as ArrayList<Message>
        isBackgroundServiceRunning = savedInstanceState.getBoolean(propSemaphoreName)
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