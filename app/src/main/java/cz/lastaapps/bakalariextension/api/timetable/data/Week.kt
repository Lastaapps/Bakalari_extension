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

import android.os.Parcelable
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.time.ZonedDateTime
import kotlin.math.min

/**contains all the data downloaded and parsed from server*/
@Parcelize
data class Week(
    val hours: DataIdList<Hour>,
    val days: ArrayList<Day>,
    val classes: DataIdList<SimpleData>,
    val groups: DataIdList<Group>,
    val subjects: DataIdList<SimpleData>,
    val teachers: DataIdList<SimpleData>,
    val rooms: DataIdList<SimpleData>,
    val cycles: DataIdList<SimpleData>,
    val loadedForDate: ZonedDateTime
) : Serializable, Parcelable {

    /**returns first day of this week*/
    @IgnoredOnParcel
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

    /**@return if week has 5 Work, Holiday or Work days*/
    fun hasValidDays(): Boolean {
        if (days.size < 5) return false
        for (day in days) {
            if (!(day.isHoliday() || day.isWorkDay()))
                return false
        }
        return true
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