package com.karpicki.blereader

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.util.*
import kotlin.concurrent.schedule

class ThingSpeakBackgroundService() : Service() {

    val API_DELAY_MILISECONDS : Long = 15000

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("ThingSpeakService", "start")
        onTaskRemoved(intent)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        Timer("NextThingSpeakSyncSet", false).schedule(API_DELAY_MILISECONDS) {
            startService(restartServiceIntent)
        }
        super.onTaskRemoved(rootIntent)
    }
}