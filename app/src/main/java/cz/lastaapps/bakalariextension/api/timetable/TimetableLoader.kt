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
import android.util.Log
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.widgets.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.ZonedDateTime

/**Obtains week object containing all the data*/
class TimetableLoader {

    companion object {
        private val TAG = TimetableLoader::class.java.simpleName

        /**Tries to load timetable for date given
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null
         *
         * For permanent timetable use date *.tools.TimeTools#PERMANENT*/
        suspend fun loadTimetable(date: ZonedDateTime, forceReload: Boolean = false): Week? {
            return withContext(Dispatchers.Default) {

                val monday = TimeTools.toMidnight(TimeTools.toMonday(date))

                return@withContext if (forceReload) {
                    loadFromServer(monday)
                } else {
                    if (shouldReload(monday)) {
                        loadFromServer(monday) ?: loadFromStorage(monday)
                    } else {
                        loadFromStorage(monday)
                    }
                }
            }
        }

        /**Tries load timetable from server and save him to local storage
         * @return downloaded Week object or null, if download failed*/
        suspend fun loadFromServer(date: ZonedDateTime): Week? {
            return withContext(Dispatchers.Default) {
                try {
                    Log.i(
                        TAG,
                        "Loading timetable from server - ${TimeTools.format(
                            date,
                            TimeTools.DATE_FORMAT
                        )}"
                    )

                    val json = withContext(Dispatchers.IO) {

                        //downloads normal or permanent timetable
                        (if (date != TimeTools.PERMANENT)
                            ConnMgr.serverGet(
                                "timetable/actual?date=${TimeTools.format(
                                    date,
                                    TimeTools.DATE_FORMAT
                                )}"
                            )
                        else
                            ConnMgr.serverGet("timetable/permanent"))
                    } ?: return@withContext null

                    //parses json
                    val week = TimetableParser.parseJson(date, json)

                    //saves json
                    TTStorage.save(date, json)

                    //updates notification service, if it is running
                    TTNotifyService.startService(App.context)
                    WidgetUpdater.update(App.context)

                    return@withContext week

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**Loads timetable from local storage
         * @return Week or null, if there is no week of the date save yet*/
        suspend fun loadFromStorage(date: ZonedDateTime): Week? {
            return withContext(Dispatchers.Default) {

                val time = TimeTools.toMidnight(TimeTools.toMonday(date))

                Log.i(
                    TAG,
                    "Loading timetable from storage - ${TimeTools.format(
                        time,
                        TimeTools.DATE_FORMAT
                    )}"
                )

                return@withContext try {

                    val json = withContext(Dispatchers.IO) { TTStorage.load(time) }
                        ?: return@withContext null

                    TimetableParser.parseJson(date, json)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }


        /**@return if the timetable for this date is outdated - should be refreshed/downloaded*/
        fun shouldReload(date: ZonedDateTime): Boolean {
            val lastUpdated = TTStorage.lastUpdated(date) ?: return true

            return (lastUpdated <= TimeTools.now.minusDays(1)
                    || date == TimeTools.PERMANENT)
        }

        /**Loads timetable from assets which is used later as example, for example in widget*/
        suspend fun loadDefault(context: Context): Week {

            return withContext(Dispatchers.Default) {
                val string = withContext(Dispatchers.IO) {

                    val stream = context.assets.open("timetable_default.json")
                    val reader = BufferedReader(InputStreamReader(stream))
                    reader.readText().also {
                        reader.close()
                    }
                }

                return@withContext TimetableParser.parseJson(TimeTools.now, JSONObject(string))
            }
        }
    }
}