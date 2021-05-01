package com.karpicki.blereader

import android.app.Service
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule

class MessageBackgroundService() : Service() {

    private val nextRunInMilliseconds : Long = 20 * Constants.second.toLong()

    private fun processMessage() {

        Log.i("THS:MessageQueue.size", MessageQueue.size().toString())

        val message = MessageQueue.get()

        if (message != null) {

            val scope = CoroutineScope(Dispatchers.IO)
            // @todo : may add results inside to broadcast to UI
            scope.launch {
                val bleItemFromDictionary = AllowedList.findByAddress(message.gatt.device.address)

                if (bleItemFromDictionary != null) {
                    sendToThingSpeak(bleItemFromDictionary, message)
                }
            }
        }

    }

    private suspend fun sendToThingSpeak(bleItemFromDictionary: AllowedItem, message: Message) {
        val value: String = when (bleItemFromDictionary.type) {
            Constants.Types.integer -> {
                readIntValue(message.characteristic).toString()
            }
            Constants.Types.float -> {
                readFloatValue(message.characteristic).toString()
            }
            Constants.Types.hex -> {
                readHexStringValue(message.characteristic)
            }
            else -> {
                readStringValue(message.characteristic)
            }
        }

        ThingSpeakClient.send(value)
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
        val format = readFormat(characteristic)
        return characteristic.getIntValue(format, 0)
    }

    private fun readFloatValue(characteristic: BluetoothGattCharacteristic): Float {
        val format = readFormat(characteristic)
        return characteristic.getFloatValue(format, 0)
    }

    private fun readStringValue(characteristic: BluetoothGattCharacteristic): String {
        Log.w("value:hexString", readHexStringValue(characteristic))
        Log.w("value:uint", readIntValue(characteristic).toString())
        return readIntValue(characteristic).toString()
    }

    private fun readFormat(characteristic: BluetoothGattCharacteristic): Int {
        val flag = characteristic.properties

        return when (flag and 0x01) {
            0x01 -> {
                BluetoothGattCharacteristic.FORMAT_UINT16
            }
            else -> {
                BluetoothGattCharacteristic.FORMAT_UINT8
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("MessageService", "start")

        processMessage()

        onTaskRemoved(intent)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        Timer("Next MessageService", false).schedule(nextRunInMilliseconds) {
            startService(restartServiceIntent)
            //startService(rootIntent)
        }
        super.onTaskRemoved(rootIntent)
    }
}