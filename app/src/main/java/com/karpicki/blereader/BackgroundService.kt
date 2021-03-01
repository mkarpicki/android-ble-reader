package com.karpicki.blereader

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import java.util.*
import kotlin.collections.ArrayList

// @readme https://gist.github.com/sam016/4abe921b5a9ee27f67b3686910293026
// known characteristics

class BackgroundService : Service() {

    private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private var mScanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    // @todo maybe temporary filter by name pattern
    // ideally (as I think possible) scan could use filter of ServiceIDs defined
    private fun isDeviceToConnect(scanResult: ScanResult): Boolean {

        val name : String? = scanResult.device.name

        if (name.isNullOrEmpty()) {
            return false
        }

        return name.contains("ESP32_")
    }

    private fun handleFoundResult(scanResult: ScanResult) {
        //Log.i("RESULT", scanResult.toString())
        if (isDeviceToConnect(scanResult)) {
            Log.i("RESULT", scanResult.toString())
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            handleFoundResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)

            if (results == null) {
                return
            }

            for(result: ScanResult in results) {
                handleFoundResult(result)
            }
        }
    }

    //9d319c9c-3abb-4b58-b99d-23c9b1b69ebc

    private fun scanLeDevices() {

//        val scanFilter :ScanFilter = ScanFilter
//            .Builder()
//            .setServiceUuid(ParcelUuid(
//                UUID.fromString("9d319c9c-3abb-4b58-b99d-23c9b1b69ebc")
//            ))
//            .build()
//
//        val filterList = ArrayList<ScanFilter>();
//        filterList.add(scanFilter)
//
//        val scanSettings :ScanSettings = ScanSettings
//            .Builder()
//            .setScanMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
//            .build()

        if (!mScanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                mScanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            mScanning = true
            //bluetoothLeScanner.startScan(filterList, scanSettings, leScanCallback)
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            mScanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //onTaskRemoved(intent)
        Toast.makeText(
            applicationContext, "This is a Service running in Background",
            Toast.LENGTH_SHORT
        ).show()

        scanLeDevices()

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
        // TODO: Return the communication channel to the service.
        //throw UnsupportedOperationException("Not yet implemented")
        return null
    }
    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}