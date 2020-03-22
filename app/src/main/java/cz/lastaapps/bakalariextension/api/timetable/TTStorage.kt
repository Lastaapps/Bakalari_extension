package cz.lastaapps.bakalariextension.api.timetable

import android.util.Log
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TTStorage {

    companion object {
        private val TAG = TTStorage::class.java.simpleName

        private const val FILE_PREFIX = "Timetable-"
        private const val FILE_SUFFIX = ".json"

        fun load(cal: ZonedDateTime): JSONObject? {
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

        fun save(cal: ZonedDateTime, json: JSONObject) {
            val file = getFile(cal)
            Log.i(TAG, "Saving ${file.name}")

            if (!file.exists()) {
                file.createNewFile()
            }

            val output = OutputStreamWriter(file.outputStream())
            output.write("${json}\n")
            output.close()
        }

        fun exists(cal: ZonedDateTime): Boolean {
            val file = getFile(cal)
            return file.exists()
        }

        fun lastUpdated(cal: ZonedDateTime): ZonedDateTime? {

            val file = getFile(cal)
            if (!file.exists())
                return null

            return ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()),
                TimeTools.UTC
            )
        }

        fun deleteAll() {
            Log.e(TAG, "Deleting ALL timetables")

            App.context.fileList().forEach {
                if (it.startsWith(FILE_PREFIX)) {
                    val f = File(App.context.filesDir, it)
                    Log.i(TAG, "Deleting ${f.name}")
                    f.deleteOnExit()
                }
            }
        }

        fun deleteOld(cal: ZonedDateTime) {
            val file = getFile(cal)
            Log.i(TAG, "Deleting older than ${file.name}")

            App.context.fileList().forEach {
                if (it.startsWith(FILE_PREFIX))
                    if (file.name > it) {
                        val f = File(App.context.filesDir, it)
                        Log.i(TAG, "Deleting ${f.name}")
                        f.deleteOnExit()
                    }
            }
        }

        private fun getFile(cal: ZonedDateTime): File {
            val time = TimeTools.toMonday(cal)

            val filename = (FILE_PREFIX + (
                if (time != TimeTools.PERMANENT) {
                    TimeTools.format(time, TimeTools.DATE_FORMAT)

                } else
                    "permanent"
                        ) + FILE_SUFFIX)
            return File(App.context.filesDir, filename)
        }
    }
}