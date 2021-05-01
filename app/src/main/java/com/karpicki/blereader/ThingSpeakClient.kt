package com.karpicki.blereader

import android.util.Log
import android.util.NoSuchPropertyException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class ThingSpeakClient {

    companion object {
        suspend fun send(value: String, field: String?) : Int =

            withContext(Dispatchers.IO) {
                var responseCode: Int

                val apiKey = BuildConfig.THING_SPEAK_API_KEY
                val field = field ?: "field1"

                if (apiKey == "") {
                    // @readme remember to set thing-speak.api-key in local.properties and
                    // let BuildConfig.java regenerate
                    throw NoSuchPropertyException("local.properties > thing-speak.api-key")
                }

                try {
                    val client = OkHttpClient();

                    // @todo - move api_key to app config
                    val request: Request = Request.Builder()
                        .url("https://api.thingspeak.com/update?api_key=$apiKey&$field=$value")
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