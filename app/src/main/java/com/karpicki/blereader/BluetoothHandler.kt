package com.karpicki.blereader

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.util.Log
import java.util.*
import okhttp3.*
import java.lang.Exception
import kotlin.collections.ArrayList

class BluetoothHandler(
    context: Context,
    private var connectedDevices: ArrayList<BluetoothDevice>
) {
    private val SERVICE_UUID = UUID.fromString("9d319c9c-3abb-4b58-b99d-23c9b1b69ebc")
    private val CHARACTERISTICS_UUID = UUID.fromString("a869a793-4b6e-4334-b1e3-eb0b74526c14")
    private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private var mScanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private var context: Context? = context

    // @todo maybe temporary filter by name pattern
    // ideally (as I think possible) scan could use filter of ServiceIDs defined
    private fun hasKnownName(scanResult: ScanResult): Boolean {

        val name : String? = scanResult.device.name

        if (name.isNullOrEmpty()) {
            return false
        }
        return name.contains("ESP32_")
    }

    private fun broadcast(gatt: BluetoothGatt, value: String) {

    }

    // @todo
    // this is example of channel (to retire)
    // build queue of requests and execute each 20 sec or less often
    private fun broadcast(gatt: BluetoothGatt, value: Int) {
        val macAddress : String = gatt.device.address
        Log.w("broadcast", "$macAddress $value")
//        var responseCode: Int
//
//        try {
//            val client = OkHttpClient();
//
//            val request: Request = Request.Builder()
//                .url("https://api.thingspeak.com/update?api_key=34CJ0H014G21EN58&field1=$value")
//                .get()
//                .build()
//
//            val response: Response = client.newCall(request).execute()
//
//            Log.d("TAG", "response.code():" + response.code())
//            //response.body()?.string()
//            responseCode = response.code()
//
//        } catch (e: Exception) {
//            responseCode = 500
//        }

        MessageList.get().add(Message())
        Log.i("BT:MessageClass.size", MessageList.get().size.toString())
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

        // @todo
        // this caused some problems so if ew wanna bring back
        // maybe notification listener must have some delay ?
        
        //gatt.readCharacteristic(characteristic)

        if (!ConnectedDevicesUtil.isConnected(connectedDevices, gatt.device)) {
            watchForChanges(gatt, characteristic)
            connectedDevices = ConnectedDevicesUtil.remember(connectedDevices, gatt.device)
        }
    }

    private fun watchForChanges(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)
        //val descriptor = characteristic.descriptors.get(0)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
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
                    connectedDevices = ConnectedDevicesUtil.forget(connectedDevices, gatt.device)
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
                    Log.w("TAG", "onServicesDiscovered received: $status")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.w("onCharacteristicChanged", "onCharacteristicChanged received")
            broadcast(gatt, readIntValue(characteristic))
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
                    broadcast(gatt, readIntValue(characteristic))
                }
                else -> {
                    Log.w("TAG", "onCharacteristicRead received: $status")
                }
            }
        }
    }

    private fun connect(device: BluetoothDevice) {
        var bluetoothGatt: BluetoothGatt? = null
        bluetoothGatt = device.connectGatt(this.context, false, gattCallback)
    }

    private fun handleFoundResult(scanResult: ScanResult) {

        if (hasKnownName(scanResult)) {
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

    fun scanLeDevices() {

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

}