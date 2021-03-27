package com.karpicki.blereader

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class ThingSpeakBackgroundService() : Service() {

    private val API_DELAY_MILISECONDS : Long = 15000

    private fun getList(intent: Intent): ArrayList<Message> {
        if (intent.extras != null) {
            return intent.extras!!.getSerializable(Constants.listName) as ArrayList<Message>
        }
        return ArrayList()
    }

    private fun processMessage(intent: Intent) {
        val messageList = getList(intent)

        Log.i("ToSend.message.size", messageList.size.toString())

        if (messageList.size == 0) {
            return
        }

//        val message = messageList.removeAt(0)
//
//        send(message)
    }

    private fun send(message: Message) {

    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("ThingSpeakService", "start")

        processMessage(intent)

        onTaskRemoved(intent)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        Timer("NextThingSpeakSyncSet", false).schedule(API_DELAY_MILISECONDS) {
            //startService(restartServiceIntent)
            startService(rootIntent)
        }
        super.onTaskRemoved(rootIntent)
    }
}