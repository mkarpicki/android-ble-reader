package com.karpicki.blereader

import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import java.util.*

// @readme https://gist.github.com/sam016/4abe921b5a9ee27f67b3686910293026
// known characteristics

class BackgroundService : Service() {

    private val SERVICE_UUID = UUID.fromString("9d319c9c-3abb-4b58-b99d-23c9b1b69ebc")
    private val CHARACTERISTICS_UUID = UUID.fromString("a869a793-4b6e-4334-b1e3-eb0b74526c14")

    private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private var mScanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    //private var connectionState = STATE_DISCONNECTED

    // @todo maybe temporary filter by name pattern
    // ideally (as I think possible) scan could use filter of ServiceIDs defined
    private fun isDeviceToConnect(scanResult: ScanResult): Boolean {

        val name : String? = scanResult.device.name

        if (name.isNullOrEmpty()) {
            return false
        }
        return name.contains("ESP32_")
    }

    private fun broadcast(gatt: BluetoothGatt, value: String) {

    }

    private fun broadcast(gatt: BluetoothGatt, value: Int) {

    }

    private fun readHexStringValue(characteristic: BluetoothGattCharacteristic): String {
        val data: ByteArray? = characteristic.value
        if (data?.isNotEmpty() == true) {
            return data.joinToString(separator = " ") {
                String.format("%02X", it)
            }
        }
        return ""
    }

    private fun readIntValue(characteristic: BluetoothGattCharacteristic): Int {
        val flag = characteristic.properties
        val format = when (flag and 0x01) {
            0x01 -> {
                BluetoothGattCharacteristic.FORMAT_UINT16
            }
            else -> {
                BluetoothGattCharacteristic.FORMAT_UINT8
            }
        }
        return characteristic.getIntValue(format, 0)
    }

    private fun readStringValue(characteristic: BluetoothGattCharacteristic): String {
        Log.w("value:hexString", readHexStringValue(characteristic))
        Log.w("value:uint", readIntValue(characteristic).toString())
        return readIntValue(characteristic).toString()
    }

    private fun read(gatt: BluetoothGatt) {
        val service : BluetoothGattService = gatt.services.find { service: BluetoothGattService ->
            service.uuid.equals(SERVICE_UUID)
        } ?: return

        Log.w("gatt.service:", service.uuid.toString())

        val characteristic: BluetoothGattCharacteristic = service.getCharacteristic(CHARACTERISTICS_UUID)
            ?: return

        Log.w("gatt.characteristics:", characteristic.uuid.toString())

        gatt.readCharacteristic(characteristic)
    }

    // https://developer.android.com/guide/topics/connectivity/bluetooth-le#kotlin
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("TAG", "Connected to GATT server.")
                    Log.i("TAG", "Attempting to start service discovery: " +
                            gatt.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("TAG", "Disconnected from GATT server.")
                }
                else -> {
                    Log.i("TAG", "ELSE from GATT server.")
                }
            }
        }
        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.w("TAG", "onServicesDiscovered received: $status")
                    read(gatt)
                }
                else -> {
                    //Log.w("TAG", "onServicesDiscovered received: $status")
                }
            }
        }

        // Result of a characteristic read operation
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.w("onCharacteristicRead", "onCharacteristicRead received: $status")
                    //readStringValue(characteristic)
                    broadcast(gatt, readIntValue(characteristic))
                }
                else -> {
                    //Log.w("TAG", "onCharacteristicRead received: $status")
                }
            }
        }
    }

    private fun connect(device: BluetoothDevice) {
        var bluetoothGatt: BluetoothGatt? = null
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private fun handleFoundResult(scanResult: ScanResult) {
        //Log.i("RESULT", scanResult.toString())
        if (isDeviceToConnect(scanResult)) {
            Log.i("RESULT", scanResult.toString())
            connect(scanResult.device)
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
        return null
    }
    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}