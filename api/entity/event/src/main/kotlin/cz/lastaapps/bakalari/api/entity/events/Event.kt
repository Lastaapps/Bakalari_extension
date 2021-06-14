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

package cz.lastaapps.bakalari.api.entity.events

import cz.lastaapps.bakalari.api.entity.core.DataId
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.searchNeutralText
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

typealias EventList = DataIdList<Event>

@Parcelize
data class Event(
    override var id: String,
    var group: Int = 0,
    val title: String,
    val description: String,
    val times: ArrayList<EventTime>,
    val eventStart: ZonedDateTime,
    val eventEnd: ZonedDateTime,
    val type: SimpleData,
    val classes: DataIdList<SimpleData>,
    //val classSets: Any,
    val teachers: DataIdList<SimpleData>,
    //val teacherSets: Any,
    val rooms: DataIdList<SimpleData>,
    //val roomSet: Any,
    val students: DataIdList<SimpleData>,
    val note: String?,
    val dateChanged: ZonedDateTime
) : DataId<String>(id), Comparable<Event> {

    companion object {
        const val GROUP_MY = 1 shl 0
        const val GROUP_PUBIC = 1 shl 1

        fun getStartTime(times: List<EventTime>): ZonedDateTime {
            var startTime =
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), TimeTools.CET)

            for (time in times) {
                if (time.start < startTime) {
                    startTime = time.start
                }
            }
            return startTime
        }

        fun getEndTime(times: List<EventTime>): ZonedDateTime {
            var endTime: ZonedDateTime =
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MIN_VALUE), TimeTools.CET)

            for (time in times) {
                if (time.end >= endTime) {
                    endTime = time.end
                }
            }
            return endTime
        }

        fun filterByText(list: List<Event>, text: String): List<Event> {
            return list.filter { event ->
                val title = event.title.searchNeutralText()
                val description = event.description.searchNeutralText()
                val note = event.note?.searchNeutralText() ?: ""
                val toSearch = text.searchNeutralText()

                if (title.contains(toSearch)
                    || description.contains(toSearch)
                    || note.contains(toSearch)
                ) return@filter true

                for (classData in event.classes)
                    if (classData.name.searchNeutralText().contains(toSearch))
                        return@filter true

                for (teacher in event.teachers)
                    if (teacher.name.searchNeutralText().contains(toSearch))
                        return@filter true

                for (room in event.rooms)
                    if (room.name.searchNeutralText().contains(toSearch))
                        return@filter true

                for (student in event.students)
                    if (student.name.searchNeutralText().contains(toSearch))
                        return@filter true

                return@filter false
            }
        }
    }

    override fun compareTo(other: Event): Int {
        return -1 * eventStart.compareTo(other.eventStart)
    }

    val isPartOfDay: Boolean
        get() {
            //plus min to make whole day 24 hours long instead of 23:59
            val duration = Duration.between(eventStart, eventEnd).plusMinutes(1)
            return (duration.toHours() < 23)
        }

    val isOneDay: Boolean
        get() {
            val duration = Duration.between(eventStart, eventEnd).plusMinutes(1)
            return (duration.toHours() in 23..25) //to fix summer and winter times
        }

    val isMoreDays: Boolean
        get() {
            val duration = Duration.between(eventStart, eventEnd).plusMinutes(1)
            return (duration.toHours() > 25)
        }
}