package com.karpicki.blereader

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class ThingSpeakClient {

    companion object {
        suspend fun send(value: String) : Int =

            withContext(Dispatchers.IO) {
                var responseCode: Int

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
    }
}