package com.karpicki.blereader

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class AllowedList {
    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context

        private var list: ArrayList<AllowedItem> = ArrayList()
        private const val allowed_list_preferences_file = "ignored_list"

        fun setContext(context: Context) {
            this.context = context
        }

        fun save(listAsString: String) {
            saveFile(listAsString)
            list = parse(listAsString)
        }

        fun get(): ArrayList<AllowedItem> {
            if (list.size == 0) {
                list = parse(loadFile())
            }
            return list
        }

        private fun loadFile(): String {

            var listStr = "[]"

            try {
                val sharedPref = context.getSharedPreferences(allowed_list_preferences_file, Context.MODE_PRIVATE)
                val listFromFile: String? = sharedPref.getString(allowed_list_preferences_file, "0")

                if (listFromFile != null) {
                    listStr = listFromFile
                }
            } catch (e: Exception) { }

            return listStr;
        }

        private fun saveFile(response: String) {
            val editor: SharedPreferences.Editor =
                context.getSharedPreferences(allowed_list_preferences_file, Context.MODE_PRIVATE).edit()

            editor.putString(allowed_list_preferences_file, response)
            editor.apply()
        }

        private fun parse(strJson: String?): ArrayList<AllowedItem> {

            var jsonArray = JSONArray()
            val list: ArrayList<AllowedItem> = ArrayList()

            if (strJson != null) {
                try {
                    jsonArray = JSONArray(strJson)
                } catch (e: JSONException) {
                }
            }

            Array(jsonArray.length()) {
                val o = (jsonArray.get(it) as JSONObject)
                val address: String = o.getString("address")
                val label: String = o.getString("label")
                val tsField: String = o.getString("tsField")
                val serviceUUID: String = o.getString("serviceUUID") //UUID.fromString(o.getString("serviceUUID"))
                val characteristicsUUID: String = o.getString("characteristicsUUID")
                var type: String = o.getString("type")

                if (!(address.isEmpty() ||
                    tsField.isEmpty() ||
                    serviceUUID.isEmpty() ||
                    characteristicsUUID.isEmpty() )) {

                        if (type.isEmpty()) {
                            type = Constants.Types.integer
                        }

                        list.add(AllowedItem(
                            address,
                            label,
                            tsField,
                            UUID.fromString(serviceUUID),
                            UUID.fromString(characteristicsUUID),
                            type
                        ))
                }
            }

            return list
        }
    }
}