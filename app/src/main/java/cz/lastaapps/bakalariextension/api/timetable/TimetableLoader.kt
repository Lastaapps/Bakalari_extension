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
import cz.lastaapps.bakalariextension.WidgetUpdater
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.services.timetablenotification.TTNotifyService
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZonedDateTime

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
        fun loadTimetable(cal: ZonedDateTime, forceReload: Boolean = false): Week? {

            val time = TimeTools.toMidnight(TimeTools.toMonday(cal))

            var toReturn: Week? = null

            if (forceReload || TTStorage.lastUpdated(time) == null) {
                toReturn = loadFromServer(time)
            } else {
                val lastUpdated = TTStorage.lastUpdated(time)
                if (lastUpdated != null)
                    if (lastUpdated.isAfter(TimeTools.now.minusDays(1))
                        || cal == TimeTools.PERMANENT
                    ) {
                        toReturn = loadFromStorage(time)
                    }

                if (toReturn == null) {
                    toReturn = loadFromServer(time)
                }
            }

            return toReturn
        }

        /**Tries load timetable from server and save him to local storage
         * @return downloaded Week object or null, if download failed*/
        fun loadFromServer(date: ZonedDateTime): Week? {
            try {
                Log.i(
                    TAG,
                    "Loading timetable from server - ${TimeTools.format(
                        date,
                        TimeTools.DATE_FORMAT
                    )}"
                )

                //downloads normal or permanent timetable
                val json = (
                        if (date != TimeTools.PERMANENT)
                            ConnMgr.serverGet(
                                "timetable/actual?date=${TimeTools.format(
                                    date,
                                    TimeTools.DATE_FORMAT
                                )}"
                            )
                        else
                            ConnMgr.serverGet("timetable/permanent")
                        ) ?: return null

                //parses json
                val week = TimetableParser.parseJson(date, json)

                //saves json
                TTStorage.save(date, json)

                //updates notification service, if it is running
                TTNotifyService.startService(App.context)
                WidgetUpdater.update(App.context)

                return week

            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**Loads timetable from local storage
         * @return Week or null, if there is no week of the date save yet*/
        fun loadFromStorage(date: ZonedDateTime): Week? {

            val time = TimeTools.toMidnight(
                TimeTools.toMonday(date)
            )

            Log.i(
                TAG,
                "Loading timetable from storage - ${TimeTools.format(time, TimeTools.DATE_FORMAT)}"
            )

            return try {

                val json = TTStorage.load(time) ?: return null
                TimetableParser.parseJson(date, json)

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}