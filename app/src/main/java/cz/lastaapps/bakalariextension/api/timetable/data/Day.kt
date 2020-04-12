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

import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZonedDateTime
import java.io.Serializable

/**Represent day of week containing lessons*/
data class Day(
    val dayOfWeek: Int,
    val date: String,
    val description: String,
    val dayType: String,
    val lessons: DataIdList<Lesson>
) : Comparable<Day>, Serializable {

    override fun compareTo(other: Day): Int {
        return TimeTools.parse(
            date, TimeTools.COMPLETE_FORMAT
        ).compareTo(
            TimeTools.parse(
                other.date, TimeTools.COMPLETE_FORMAT
            )
        )
    }

    /**@param hour which lesson should be returned
     * @param cycle (optional) returns lesson for specific week, if null, returns first found
     * @return lesson if there is OR WAS lesson (removed), of null, if cell is empty*/
    fun getLesson(hour: Hour, cycle: TTData.Cycle? = null): Lesson? {
        if (cycle == null)
            return lessons.getById(hour.id)

        val array = lessons.getAllById(hour.id)
        for (lesson in array) {
            if (lesson.cycleIds.contains(cycle.id))
                return lesson
        }

        //free(removed) and absence lessons have empty cycle ids
        for (lesson in array) {
            if (lesson.isAbsence() || lesson.isRemoved())
                return lesson
        }

        return null
    }

    /**@return if this lesson if empty or removed*/
    fun isFree(hour: Hour, cycle: TTData.Cycle? = null): Boolean {
        val lesson = getLesson(hour, cycle) ?: return true
        if (lesson.isRemoved()) return true
        return false
    }

    /**@return if lesson is regular of added*/
    fun isNormal(hour: Hour, cycle: TTData.Cycle? = null): Boolean {
        val lesson = getLesson(hour, cycle) ?: return false
        return lesson.isNormal()
    }

    /**@return if current lesson is absence*/
    fun isAbsence(hour: Hour, cycle: TTData.Cycle? = null): Boolean {
        val lesson = getLesson(hour, cycle) ?: return false
        return lesson.isAbsence()
    }

    /**@return index of the first not empty lesson of the day*/
    fun firstLessonIndex(hours: DataIdList<Hour>, cycle: TTData.Cycle? = null): Int {
        for (i in 0 until hours.size) {
            if (!isFree(hours[i], cycle))
                return i
        }
        return -1
    }

    /**@return index of the last not empty lesson of the day*/
    fun lastLessonIndex(hours: DataIdList<Hour>, cycle: TTData.Cycle? = null): Int {
        for (i in (hours.size - 1) downTo 0) {
            if (!isFree(hours[i], cycle))
                return i
        }
        return -1
    }

    /**@return true, if there is no free lesson between #firstLessonIndex and #lastLessonIndex*/
    fun endsWithLunch(hours: DataIdList<Hour>, cycle: TTData.Cycle? = null): Boolean {
        for (i in firstLessonIndex(hours)..lastLessonIndex(hours)) {
            if (i > 0) return false

            if (isFree(hours[i], cycle)) {
                return false
            }
        }
        return true
    }

    /**@return if there is normal of absence lessons*/
    fun isEmpty(): Boolean {
        for (lesson in lessons) {
            if (!lesson.isRemoved()) {
                return false
            }
        }
        return true
    }

    /**@return if day is holiday (and also empty)*/
    fun isHoliday(): Boolean {
        return dayType == "Celebration" || dayType == "Holiday"
    }

    /**@return Special day description or default string*/
    fun getHolidayDescription(): String {
        return if (description != "") {
            description
        } else {
            App.getString(R.string.holiday)
        }
    }

    /**caches parsed date*/
    private var _toDate: ZonedDateTime? = null

    /**parses current date*/
    fun toDate(): ZonedDateTime {
        if (_toDate == null) {
            _toDate = if (date == "") {
                TimeTools.PERMANENT
            } else {
                TimeTools.parse(date, TimeTools.COMPLETE_FORMAT)
            }
        }
        return _toDate!!
    }

    override fun toString(): String {
        return "{Day: $date $lessons}"
    }
}
