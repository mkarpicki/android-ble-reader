package com.karpicki.blereader

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.util.NoSuchPropertyException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.*
import kotlin.concurrent.schedule

class DeviceAllowedListReaderService: Service() {

    private val nextRunInMilliseconds : Long = Constants.hour.toLong()

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private  fun load() {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val listResponse = get()
            if (listResponse != null) {
                setDictionary(listResponse)
            }
        }
    }

    private fun setDictionary (responseStr: String) {
        AllowedList.save(responseStr)
    }

    suspend fun get() : String? =

        withContext(Dispatchers.IO) {

            var responseStr: String? = null

            val apiKey = BuildConfig.MY_DEVICES_API_KEY
            val url = BuildConfig.MY_DEVICES_URL

            if (url == "") {
                throw NoSuchPropertyException("local.properties > home-devices-api.url")
            }

            if (apiKey == "") {
                throw NoSuchPropertyException("local.properties > home-devices-api.api-key")
            }

            try {
                val client = OkHttpClient();

                val request: Request = Request.Builder()
                    .url(url)
                    .get()
                    .header("x-api-key", apiKey )
                    .build()

                val response: Response = client.newCall(request).execute()

                Log.d("DeviceList.responseCode", "response.code():" + response.code())

                if (response.code() == 200) {
                    responseStr = response.body()!!.string()
                } else {
                    throw Exception(response.code().toString())
                }

            } catch (e: Exception) {
                Log.e("DeviceList.loading", e.toString())

            }
            responseStr
        }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("DeviceListService", "start")

        load()

        onTaskRemoved(intent)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        Timer("Next DeviceListService", false).schedule(nextRunInMilliseconds) {
            startService(restartServiceIntent)
            //startService(rootIntent)
        }
        super.onTaskRemoved(rootIntent)
    }
}