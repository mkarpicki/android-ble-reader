package com.karpicki.blereader

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException

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
            parse(listAsString)
        }

        fun get() {
            parse(loadFile())
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

        private fun parse(strJson: String?): Array<String> {

            var jsonArray = JSONArray()

            if (strJson != null) {
                try {
                    jsonArray = JSONArray(strJson)
                } catch (e: JSONException) {
                }
            }

            val array =  Array(jsonArray.length()) {
                jsonArray.getString(it)
            }

            return array
        }
    }
}