package com.karpicki.blereader

import android.app.Service
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.*
import kotlin.concurrent.schedule

class MessageBackgroundService() : Service() {

    private val nextRunInMilliseconds : Long = 20 * Constants.second.toLong()

    private fun processMessage() {
        val messageList = MessageList.get()

        Log.i("THS:MessageClass.size", MessageList.get().size.toString())

        if (messageList.size == 0) {
            return
        }

        val message = messageList.removeAt(0)
        val scope = CoroutineScope(Dispatchers.IO)
        // @todo : may add results inside to broadcast to UI
        scope.launch { send(message) }
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

    private suspend fun send(message: Message) : Int =

        withContext(Dispatchers.IO) {
        var responseCode: Int
        val value = readStringValue(message.characteristic)

        try {
            val client = OkHttpClient();

            // @todo - move api_key to app config
            val request: Request = Request.Builder()
                .url("https://api.thingspeak.com/update?api_key=34CJ0H014G21EN58&field1=$value")
                .get()
                .build()

            val response: Response = client.newCall(request).execute()

            Log.d("TAG", "response.code():" + response.code())
            //response.body()?.string()
            responseCode = response.code()

        } catch (e: Exception) {
            responseCode = 500
        }
        responseCode
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("ThingSpeakService", "start")

        processMessage()

        onTaskRemoved(intent)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        Timer("Next ThingSpeakBackgroundService", false).schedule(nextRunInMilliseconds) {
            startService(restartServiceIntent)
            //startService(rootIntent)
        }
        super.onTaskRemoved(rootIntent)
    }
}