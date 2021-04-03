package com.karpicki.blereader

import android.app.Service
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

class ThingSpeakBackgroundService() : Service() {

    private val API_DELAY_MILISECONDS : Long = 16000

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

    private suspend fun send(message: Message) : Int =

        withContext(Dispatchers.IO) {
        var responseCode: Int
        val value = message.value

        try {
            val client = OkHttpClient();

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

        Timer("NextThingSpeakSyncSet", false).schedule(API_DELAY_MILISECONDS) {
            startService(restartServiceIntent)
            //startService(rootIntent)
        }
        super.onTaskRemoved(rootIntent)
    }
}