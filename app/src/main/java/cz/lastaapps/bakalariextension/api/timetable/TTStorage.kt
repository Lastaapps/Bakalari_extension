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

package cz.lastaapps.bakalariextension.api.timetable

import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.Timer
import org.json.JSONObject
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime
import org.threeten.bp.ZonedDateTime
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**Manages storing of timetable JSONs*/
class TTStorage {

    companion object {
        private val TAG = TTStorage::class.java.simpleName

        private const val FILE_PREFIX = "Timetable-"
        private const val FILE_SUFFIX = ".json"
        private val FILE_NAME_FORMAT = TimeTools.DATE_FORMAT
        private const val PERMANENT = "permanent"

        private var actualWeekCache: JSONObject? = null
        fun releaseActualCache() {
            actualWeekCache = null
        }

        /**Tries to load timetable for date given
         * @return json or null, if there isn't such a week saved*/
        fun load(cal: ZonedDateTime): JSONObject? {

            //TODO remove
            val timer = Timer(TAG)

            //check if the request is made for current week
            val isActual = isActual(cal)

            //and tries to load it from cache
            if (isActual && actualWeekCache != null)
                return actualWeekCache

            //gets file name
            val file = getFile(cal)
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
            if (isActual) {
                actualWeekCache = json
            }

            timer.print()

            return json
        }

        /**saves json for date given*/
        fun save(cal: ZonedDateTime, json: JSONObject) {

            //updates cache
            if (isActual(cal))
                actualWeekCache = json

            //gets new name
            val file = getFile(cal)
            Log.i(TAG, "Saving ${file.name}")

            if (!file.exists()) {
                file.createNewFile()
            }

            //writes data to file
            val output = OutputStreamWriter(file.outputStream())
            output.write("${json}\n")
            output.close()
        }

        /**@return if date is contained in current week*/
        private fun isActual(date: ZonedDateTime): Boolean =
            TimeTools.toDate(TimeTools.toMonday(date)) == TimeTools.monday.toLocalDate()

        /**@return if there is saved timetable for this date*/
        fun exists(cal: ZonedDateTime): Boolean {
            val file = getFile(cal)
            return file.exists()
        }

        /**@return when was timetable last updated, of null if it doesn't exist*/
        fun lastUpdated(cal: ZonedDateTime): ZonedDateTime? {

            val file = getFile(cal)
            if (!file.exists())
                return null

            return ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()),
                TimeTools.UTC
            )
        }

        /**deletes all saved timetables*/
        fun deleteAll() {
            Log.e(TAG, "Deleting ALL timetables")

            releaseActualCache()

            App.context.fileList().forEach {
                if (it.startsWith(FILE_PREFIX)) {
                    val f = File(App.context.filesDir, it)
                    Log.i(TAG, "Deleting ${f.name}")
                    f.deleteOnExit()
                }
            }
        }

        /**deletes all timetables older than*/
        fun deleteOld(cal: ZonedDateTime) {
            val file = getFile(cal)
            Log.i(TAG, "Deleting older than ${file.name}")

            App.context.fileList().forEach {
                if (it.startsWith(FILE_PREFIX))
                    if (file.name > it) {
                        val f = File(App.context.filesDir, it)
                        Log.i(TAG, "Deleting ${f.name}")
                        f.delete()
                    }
            }
        }

        /**returns all timetables dates*/
        fun getAll(): ArrayList<ZonedDateTime> {
            val list = ArrayList<ZonedDateTime>()

            App.context.fileList().forEach {
                if (it.startsWith(FILE_PREFIX)) {
                    val f = File(App.context.filesDir, it)

                    val name = f.name.replace(FILE_PREFIX, "")
                        .replace(FILE_SUFFIX, "")

                    if (name == PERMANENT)
                        list.add(TimeTools.PERMANENT)
                    else
                        list.add(
                            ZonedDateTime.of(
                                TimeTools.parseDate(name, FILE_NAME_FORMAT),
                                LocalTime.MIDNIGHT, TimeTools.UTC
                            )
                        )
                }
            }
            return list
        }

        /**Returns file name for date given*/
        private fun getFile(date: ZonedDateTime): File {
            val monday = TimeTools.toMonday(date)

            val filename = (FILE_PREFIX + (
                    if (monday != TimeTools.PERMANENT) {
                        TimeTools.format(monday, FILE_NAME_FORMAT)

                    } else
                        PERMANENT
                    ) + FILE_SUFFIX)
            return File(App.context.filesDir, filename)
        }
    }
}