package com.karpicki.blereader

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.Button
import java.util.*

class MainActivity : AppCompatActivity() {

    private val propName = "MESSAGE_LIST"
    var messageList = ArrayList<Message>()

    private fun start() {
        val queue = PriorityQueue<Message>()
        val bleServiceIntent = Intent(applicationContext, BLEBackgroundService::class.java)
        val thingSpeakIntent = Intent(applicationContext, ThingSpeakBackgroundService::class.java)

        bleServiceIntent.putExtra(propName, queue)
        thingSpeakIntent.putExtra(propName, queue)

        startService(bleServiceIntent)
        startService(thingSpeakIntent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save the user's current game state
        outState.run {
            putSerializable(propName, messageList)
        }
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) { // Here You have to restore count value
        super.onRestoreInstanceState(savedInstanceState)
        messageList = savedInstanceState.getSerializable(propName) as ArrayList<Message>
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