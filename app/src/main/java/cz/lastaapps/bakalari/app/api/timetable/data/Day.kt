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

package cz.lastaapps.bakalari.app.api.timetable.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.DataIdList
import cz.lastaapps.bakalari.app.api.SimpleData
import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.platform.App
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**Represent day of week containing lessons*/
@Parcelize
@Entity(tableName = APIBase.TIMETABLE_DAY)
data class Day(
    val dayOfWeek: Int,
    @PrimaryKey
    @ColumnInfo(index = true)
    val date: LocalDate,
    val description: String, //never contains emojis, used for widget and notifications
    val descriptionEmoji: String, //description with emoji on celebrations
    val dayType: String,
    @Ignore
    val lessons: DataIdList<Lesson>
) : Comparable<Day>, Serializable, Parcelable {

    @Deprecated("For Room initialization only")
    constructor(
        dayOfWeek: Int,
        date: LocalDate,
        description: String,
        descriptionEmoji: String,
        dayType: String,
    ) : this(dayOfWeek, date, description, descriptionEmoji, dayType, DataIdList())

    override fun compareTo(other: Day): Int {
        return date.compareTo(other.date)
    }

    /**@param hour which lesson should be returned
     * @param cycle (optional) returns lesson for specific week, if null, returns first found
     * @return lesson if there is OR WAS lesson (removed), of null, if cell is empty*/
    fun getLesson(hour: Hour, cycle: SimpleData? = null): Lesson? {
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
    fun isFree(hour: Hour, cycle: SimpleData? = null): Boolean {
        val lesson = getLesson(hour, cycle) ?: return true
        if (lesson.isRemoved()) return true
        return false
    }

    /**@return if lesson is regular of added*/
    fun isNormal(hour: Hour, cycle: SimpleData? = null): Boolean {
        val lesson = getLesson(hour, cycle) ?: return false
        return lesson.isNormal()
    }

    /**@return if current lesson is absence*/
    fun isAbsence(hour: Hour, cycle: SimpleData? = null): Boolean {
        val lesson = getLesson(hour, cycle) ?: return false
        return lesson.isAbsence()
    }

    /**@return index of the first not empty lesson of the day*/
    fun firstLessonIndex(hours: DataIdList<Hour>, cycle: SimpleData? = null): Int {
        if (isWorkDay())
            for (i in 0 until hours.size) {
                if (!isFree(hours[i], cycle))
                    return i
            }
        else if (isHoliday()) {
            return HOLIDAY_INDEX
        }
        return -1
    }

    /**@return index of the last not empty lesson of the day*/
    fun lastLessonIndex(hours: DataIdList<Hour>, cycle: SimpleData? = null): Int {
        if (isWorkDay())
            for (i in (hours.size - 1) downTo 0) {
                if (!isFree(hours[i], cycle))
                    return i
            }
        else if (isHoliday()) {
            return HOLIDAY_INDEX //minimum timetable size to show holiday
        }
        return -1
    }

    /**@return true, if there is no free lesson between #firstLessonIndex and #lastLessonIndex*/
    fun endsWithLunch(hours: DataIdList<Hour>, cycle: SimpleData? = null): Boolean {
        for (i in firstLessonIndex(hours)..lastLessonIndex(hours)) {
            if (i < 0) return false

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

    /**@return if day is workday*/
    fun isWorkDay(): Boolean {
        return dayType == WORKDAY
    }

    /**@return if day is holiday (and also empty)*/
    fun isHoliday(): Boolean {
        //return dayType in listOf(CELEBRATION, HOLIDAY, DIRECTOR_DAY)
        return dayType !in listOf(WORKDAY, WEEKEND)
    }

    /**@return if day is weekend (and also empty)
     * weekend is not normally presented in data set, but can occur in some abnormal situations,
     * like the beginning of the school year*/
    fun isWeekend(): Boolean {
        return dayType == WEEKEND
    }

    /**@return Special day description or default string*/
    fun getHolidayDescription(emoji: Boolean = false): String {
        return if (description != "") {
            if (!emoji) description else descriptionEmoji
        } else {
            if (dayType == CELEBRATION)
                App.getString(R.string.timetable_celebration)
            else
                App.getString(R.string.timetable_holiday)
        }
    }

    override fun toString(): String {
        return "{Day: ${date.format(DateTimeFormatter.BASIC_ISO_DATE)} $lessons}"
    }

    companion object {

        const val WORKDAY = "WorkDay"
        const val WEEKEND = "Weekend"
        const val CELEBRATION = "Celebration"
        const val HOLIDAY = "Holiday"
        const val DIRECTOR_DAY = "DirectorDay"
        val dayTypes = listOf(WORKDAY, WEEKEND, CELEBRATION, HOLIDAY, DIRECTOR_DAY)

        //used in lessons first and last lessons for the first and last indexes
        private const val HOLIDAY_INDEX = -2

        fun getEmptyDay(dayOfWeek: Int, date: LocalDate): Day =
            Day(dayOfWeek, date, "", "", HOLIDAY)
    }
}
