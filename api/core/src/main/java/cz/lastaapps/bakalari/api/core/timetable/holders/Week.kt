/*
 *    Copyright 2021, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalari.api.core.timetable.holders

import android.os.Parcelable
import cz.lastaapps.bakalari.api.core.DataIdList
import cz.lastaapps.bakalari.api.core.SimpleData
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toDate
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.LocalDate
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
    val monday: LocalDate
) : Serializable, Parcelable {

    /**Ignores entries like teachers, subjects, rooms...
     * Made to make queries from database simpler to write,
     * the queries fetch all the data available*/
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Week) return false

        if (days != other.days) return false
        if (monday != other.monday) return false

        return true
    }

    /**Ignores entries like teachers, subjects, rooms...
     * Made to make queries from database simpler to write,
     * the queries fetch all the data available*/
    override fun hashCode(): Int {
        var result = hours.hashCode()
        result = 31 * result + days.hashCode()
        result = 31 * result + monday.hashCode()
        return result
    }

    /**@return day of the week for date given
     * or null, if date is out of the week range or is weekend
     */
    fun getDay(date: LocalDate): Day? {

        for (day in days)
            if (day.date == date)
                return day

        return null
    }

    /**@return day of the week for index given
     * or null, if there is no such a day
     */
    fun getDayOfWeek(dayOfWeek: Int): Day? {

        for (day in days)
            if (day.dayOfWeek == dayOfWeek)
                return day

        return null
    }

    /**@return if week has 5 Work, Holiday or Work days*/
    fun hasValidDays(): Boolean {
        for (day in days) {
            if (!day.isWeekend())
                return true
        }
        return false
    }

    /**@return today's day object if today is contained in day list*/
    fun today(): Day? {
        return getDay(TimeTools.now.toDate(TimeTools.CET))
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
        if (min in 0 until hours.size) {
            data.addAll(hours.subList(min, hours.size))
        } else {
            for (day in days) {
                if (day.isHoliday()) {
                    data.addAll(hours)
                    break
                }
            }
        }

        return data
    }

    /**If timetable is permanent*/
    fun isPermanent(): Boolean {
        return monday == TimeTools.PERMANENT
    }
}