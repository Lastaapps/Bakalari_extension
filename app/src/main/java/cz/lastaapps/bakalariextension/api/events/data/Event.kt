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

package cz.lastaapps.bakalariextension.api.events.data

import cz.lastaapps.bakalariextension.api.DataId
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.tools.TimeTools
import java.time.Duration
import java.time.ZonedDateTime

typealias EventList = DataIdList<Event>

//TODO @Parcelize - *sets needed
class Event(
    override var id: String,
    var group: Int = 0,
    var title: String,
    var description: String,
    var times: ArrayList<EventTime>,
    var type: SimpleData,
    var classes: DataIdList<SimpleData>,
    var classSets: Any,
    var teachers: DataIdList<SimpleData>,
    var teacherSets: Any,
    var rooms: DataIdList<SimpleData>,
    var roomSet: Any,
    var students: DataIdList<SimpleData>,
    var note: Any?,
    var dateChanged: String
) : DataId<String>(id), Comparable<Event> {

    companion object {
        const val GROUP_MY = 1 shl 0
        const val GROUP_PUBIC = 1 shl 1
    }

    override fun compareTo(other: Event): Int {
        return -1 * eventStart.compareTo(other.eventStart)
    }

    val date: ZonedDateTime = TimeTools.parse(dateChanged, TimeTools.COMPLETE_FORMAT)

    val eventStart: ZonedDateTime

    val eventEnd: ZonedDateTime

    init {
        var firstTime: ZonedDateTime = TimeTools.lastJune.plusDays(1)
        var lastTime: ZonedDateTime = TimeTools.firstSeptember

        for (time in times) {
            if (time.dateStart < firstTime) {
                firstTime = time.dateStart
            }
            if (time.dateEnd > lastTime) {
                lastTime = time.dateEnd
            }
        }

        eventStart = firstTime
        eventEnd = lastTime
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