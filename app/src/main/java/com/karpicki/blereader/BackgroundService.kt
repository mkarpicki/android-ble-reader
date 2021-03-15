package com.karpicki.blereader

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import java.util.*


// @readme https://gist.github.com/sam016/4abe921b5a9ee27f67b3686910293026
// known characteristics

class BackgroundService : Service() {

    private var bluetoothHandler : BluetoothHandler? = null
    private var connectedDevices: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //onTaskRemoved(intent)
        Toast.makeText(
            applicationContext, "This is a Service running in Background",
            Toast.LENGTH_SHORT
        ).show()

        if (bluetoothHandler == null) {
            bluetoothHandler = BluetoothHandler(this, connectedDevices)
        }
        bluetoothHandler!!.scanLeDevices()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(
            applicationContext, "Service onDestroy",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}