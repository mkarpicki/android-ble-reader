package com.karpicki.blereader

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList

class BluetoothHandler(
    context: Context,
    private var connectedDevices: ArrayList<BluetoothDevice>
) {
    private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private var mScanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val scanPeriod: Long = 10 * Constants.second.toLong()

    private var context: Context? = context

    private fun hasKnownAddress(scanResult: ScanResult): Boolean {

        val address: String = scanResult.device.address

        return (AllowedList.filterByAddress(address).size > 0)
    }

    private fun broadcast(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {

        MessageQueue.insert(Message(gatt, characteristic))
        Log.i("BT:Broadcast:dev", gatt.device.toString())
        Log.i("BT:MessageQueue.size", MessageQueue.size().toString())
    }

    private fun read(gatt: BluetoothGatt) {

        val address: String = gatt.device.address

        gatt.services.forEach { service ->

            val devicesInDictionary = AllowedList.filterByAddressAndServiceUUID(address, service.uuid)

            if (devicesInDictionary.size > 0) {
                Log.w("gatt.service:", service.uuid.toString())

                devicesInDictionary.forEach { deviceInDictionary ->
                    val characteristic: BluetoothGattCharacteristic = service.getCharacteristic(
                      deviceInDictionary.characteristicsUUID
                    ) ?: return

                    Log.w("gatt.characteristics:", characteristic.uuid.toString())

                    // @todo
                    // this caused some problems so if ew wanna bring back
                    // maybe notification listener must have some delay ?

                    //gatt.readCharacteristic(characteristic)

                    watchForChanges(gatt, characteristic)
                    connectedDevices = ConnectedDevicesUtil.remember(connectedDevices, gatt.device)
                }
            }
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
            broadcast(gatt, characteristic)
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
                    broadcast(gatt, characteristic)
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

        if (hasKnownAddress(scanResult)) {

            Log.i("RESULT", scanResult.toString())
            if (!ConnectedDevicesUtil.isConnected(connectedDevices, scanResult.device)) {
                connect(scanResult.device)
            } else {
                Log.i("ALREADY CONNECTED", scanResult.device.address)
            }
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
            }, scanPeriod)
            mScanning = true
            //bluetoothLeScanner.startScan(filterList, scanSettings, leScanCallback)
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            mScanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

}