package cz.lastaapps.bakalariextension.api.timetable

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.tools.App
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*

class TTStorage {

    companion object {
        private val TAG = TTStorage::class.java.simpleName

        private const val FILE_PREFIX = "Timetable-"
        private const val FILE_SUFFIX = ".json"

        private const val SP_KEY = "TIMETABLE_STORAGE"

        fun load(cal: Calendar): JSONObject? {
            val file = getFile(cal)
            Log.i(TAG, "Loading ${file.name}")

            if (!file.exists())
                return null

            val input = file.inputStream()
            val br = BufferedReader(InputStreamReader(input))
            var data = ""
            var line: String?
            while (br.readLine().also { line = it } != null) {
                data += line
            }
            br.close()

            return JSONObject(data)
        }

        fun save(cal: Calendar, json: JSONObject) {
            val file = getFile(cal)
            Log.i(TAG, "Saving ${file.name}")

            if (!file.exists()) {
                file.createNewFile()
            }

            val output = OutputStreamWriter(file.outputStream())
            output.write("${json}\n")
            output.close()

            getSP().edit {
                putLong(file.name, Date().time)
                apply()
            }
        }

        fun exists(cal: Calendar): Boolean {
            val file = getFile(cal)
            return file.exists()
        }

        fun lastUpdated(cal: Calendar): Calendar {
            val time = getSP().getLong(getFile(cal).name, 0)
            val c = Calendar.getInstance()
            c.time = Date(time)
            return c
        }

        fun deleteOld(cal: Calendar) {
            val file = getFile(cal)
            Log.i(TAG, "Deleting older than ${file.name}")

            App.appContext().fileList().forEach {
                if (it.startsWith(FILE_PREFIX))
                    if (file.name > it) {
                        val f = File(App.appContext().filesDir, it)
                        Log.i(TAG, "Deleting ${f.name}")
                        f.deleteOnExit()

                        getSP().edit {
                            remove(f.name)
                            apply()
                        }
                    }
            }
        }

        private fun getFile(cal: Calendar): File {
            TTTools.toMonday(cal)
            val filename = FILE_PREFIX + TTTools.format(cal) + FILE_SUFFIX
            return File(App.appContext().filesDir, filename)
        }

        private fun getSP(): SharedPreferences {
            return App.appContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
        }
    }
}