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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.collection.LruCache
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.database.APIBase
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toDate
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toMonday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate

class TimetableMainRepository(private val database: APIBase) {

    private val dao = database.timetableDao()

    /**@return mondays of week downloaded*/
    fun getWeeks() = dao.getWeeks().filter { it.dayOfWeek == DayOfWeek.MONDAY }

    /**contains references to already initialized repositories*/
    private val referenceMap = LruCache<LocalDate, TimetableRepository>(REPO_CACHE)

    /**gets repository for the date given*/
    fun getRepositoryForDate(date: LocalDate): TimetableRepository {
        val monday = date.toMonday()
        val repo: TimetableRepository

        val mapItem = referenceMap[date]
        if (mapItem == null) {
            repo = TimetableRepository(database, monday)
            referenceMap.put(date, repo)
        } else {
            repo = mapItem
        }

        return repo
    }

    /**If common timetable is supported by school*/
    suspend fun isWebTimetableAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(ConnMgr.getRawUrl() + "/timetable/public")
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "HEAD"
            urlConnection.connectTimeout = 15 * 1000
            urlConnection.readTimeout = 15 * 1000
            urlConnection.setRequestProperty("Connection", "Close")
            urlConnection.disconnect()

            //when page is not found or redirect is returned
            return@withContext urlConnection.responseCode == 200 && urlConnection.url == url

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openWebTimetable(context: Context) = openWebTimetable(context, "", "", "")
    fun openWebTimetable(context: Context, date: String, type: String, id: String) {
        val url = ConnMgr.getRawUrl() + "/timetable/public/" + date + type + id

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(browserIntent)
    }

    companion object {

        private const val REPO_CACHE = 8 * 1024 * 1024

        const val DATE_PERMANENT = "permanent/"
        const val DATE_ACTUAL = "actual/"
        const val DATE_NEXT = "next/"
        const val TYPE_CLASS = "class/"
        const val TYPE_TEACHER = "teacher/"
        const val TYPE_ROOM = "room/"

        /**Loads timetable from assets which is used later as example, for example in widget*/
        fun loadDefault(context: Context): Week {

            val stream = context.assets.open("timetable_default.json")
            val reader = BufferedReader(InputStreamReader(stream))

            val string = reader.readText().also {
                reader.close()
            }

            return TimetableParser.parseJson(TimeTools.now.toDate(), JSONObject(string))
        }
    }
}