/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Bakalari extension.
 *
 *     Bakalari extension is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Bakalari extension is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Bakalari extension.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package cz.lastaapps.bakalariextension.api.events

import android.content.Intent
import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZonedDateTime

/**Manages events saving and loading*/
class EventsStorage {

    companion object {
        private val TAG = EventsStorage::class.java.simpleName

        private const val FILE_PREFIX = "Events"
        private const val FILE_SUFFIX = ".json"

        private var cache = HashMap<String, JSONObject?>()
        private fun releaseCache() {
            cache.clear()
        }

        /**Tries to load data
         * @return json or null, if there isn't data saved*/
        fun load(type: String): JSONObject? {

            if (cache[type] != null)
                return cache[type]

            //gets file name
            val file = getFile(type)
            Log.i(TAG, "Loading ${file.name}")

            if (!file.exists())
                return null

            //reads data
            val input = file.inputStream()
            val br = BufferedReader(InputStreamReader(input))
            var data = ""
            var line: String?
            while (br.readLine().also { line = it } != null) {
                data += line
            }
            br.close()

            //caches loaded data to speed up app
            val json = JSONObject(data)

            cache[type] = json

            return json
        }

        /**saves json*/
        fun save(json: JSONObject, type: String) {
            try {

                cache[type] = json

                //gets new name
                val file = getFile(type)
                Log.i(TAG, "Saving ${file.name}")

                if (!file.exists()) {
                    file.createNewFile()
                }

                //writes data to file
                val output = OutputStreamWriter(file.outputStream())
                output.write("${json}\n")
                output.close()

            } catch (e: Exception) {
                e.printStackTrace()
                App.context.sendBroadcast(Intent(MainActivity.FULL_STORAGE))
            }
        }

        /**@return when was data last updated, of null if they aren't saved yet*/
        fun lastUpdated(type: String): ZonedDateTime? {

            val file = getFile(type)
            if (!file.exists())
                return null

            return ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()),
                TimeTools.UTC
            )
        }

        /**deletes saved homework*/
        fun delete(type: String) {
            Log.e(TAG, "Deleting $type")

            getFile(type).delete()
            releaseCache()
        }

        /**Returns file name*/
        private fun getFile(type: String): File {
            val filename = ("$FILE_PREFIX-$type$FILE_SUFFIX")
            return File(App.context.filesDir, filename)
        }
    }
}