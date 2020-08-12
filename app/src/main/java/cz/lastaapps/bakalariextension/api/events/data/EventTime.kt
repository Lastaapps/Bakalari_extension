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

import android.os.Parcelable
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
class EventTime(
    var wholeDay: Boolean,
    var startTime: String,
    var endTime: String
) : Parcelable {

    val dateStart: ZonedDateTime = parseTime(startTime)
    val dateEnd: ZonedDateTime = parseTime(endTime)

    //time formats differ not sure why and when, to try catch is used
    private fun parseTime(time: String): ZonedDateTime {
        return try {
            TimeTools.parse(time, TimeTools.COMPLETE_FORMAT)
        } catch (e: Exception) {
            TimeTools.parse(time, TimeTools.COMPLETE_SHORTER)
        }
    }
}