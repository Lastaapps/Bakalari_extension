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

package cz.lastaapps.bakalariextension.api.timetable.data

import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZonedDateTime
import java.io.Serializable
import kotlin.math.min

/**contains all the data downloaded and parsed from server*/
data class Week(
    val hours: DataIdList<Hour>,
    val days: ArrayList<Day>,
    val classes: DataIdList<Class>,
    val groups: DataIdList<Group>,
    val subjects: DataIdList<Subject>,
    val teachers: DataIdList<Teacher>,
    val rooms: DataIdList<Room>,
    val cycles: DataIdList<Cycle>,
    val loadedForDate: ZonedDateTime
): Serializable {

    /**returns first day of this week*/
    val monday: ZonedDateTime = loadedForDate

    /**@return day of the week for date selected
     * or null, if date is out of the week range or is weekend
     */
    fun getDay(cal: ZonedDateTime): Day? {

        return if (TimeTools.toDateTime(days[0].toDate()) <= TimeTools.toDateTime(cal)
            && TimeTools.toDateTime(cal) < TimeTools.toDateTime(days[4].toDate()).plusDays(1)
        )
            days[cal.dayOfWeek.value - 1]
        else
            null
    }

    /**@return today's day object if today is contained in day list*/
    fun today(): Day? {
        return getDay(TimeTools.now)
    }

    /**trims hours from beginning, where there is no lesson during whole week*/
    fun trimFreeMorning(): DataIdList<Hour> {
        var min = hours.size
        for (day in days) {
            val firstLesson = day.firstLessonIndex(hours)
            if (firstLesson >= 0)
                min = min(firstLesson, min)
        }

        val data = DataIdList<Hour>()
        if (min in 0 until hours.size)
            data.addAll(hours.subList(min, hours.size))

        return data
    }

    /**If timetable is permanent*/
    fun isPermanent():Boolean {
        return loadedForDate == TimeTools.PERMANENT
    }
}