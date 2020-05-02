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

package cz.lastaapps.bakalariextension.api.marks

import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/***/
class MarksStorage {

    companion object {
        private val TAG = MarksStorage::class.java.simpleName

        private const val FILE_PREFIX = "Marks"
        private const val FILE_SUFFIX = ".json"

        private var marksCache: JSONObject? = null
        fun releaseCache() {
            marksCache = null
        }

        /**Tries to load timetable for date given
         * @return json or null, if there isn't such a week saved*/
        fun load(): JSONObject? {

            if (marksCache != null)
                return marksCache

            //gets file name
            val file = getFile()
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

            //if the request is made for current week, saves it to cache
            val json = JSONObject(data)

            marksCache = json

            return json
        }

        /**saves json for date given*/
        fun save(json: JSONObject) {

            marksCache = json

            //gets new name
            val file = getFile()
            Log.i(TAG, "Saving ${file.name}")

            if (!file.exists()) {
                file.createNewFile()
            }

            //writes data to file
            val output = OutputStreamWriter(file.outputStream())
            output.write("${json}\n")
            output.close()
        }

        /**@return if there are saved marks*/
        fun exists(): Boolean {
            val file = getFile()
            return file.exists()
        }

        /**@return when was marks last updated, of null if they aren't saned yet*/
        fun lastUpdated(): ZonedDateTime? {

            val file = getFile()
            if (!file.exists())
                return null

            return ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()),
                TimeTools.UTC
            )
        }

        /**deletes all saved timetables*/
        fun delete() {
            Log.e(TAG, "Deleting marks")

            getFile().delete()
            releaseCache()
        }

        /**Returns file name*/
        private fun getFile(): File {
            val filename = (FILE_PREFIX + FILE_SUFFIX)
            return File(App.context.filesDir, filename)
        }
    }
}