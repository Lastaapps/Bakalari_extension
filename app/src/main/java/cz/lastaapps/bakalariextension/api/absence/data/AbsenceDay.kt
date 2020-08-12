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

package cz.lastaapps.bakalariextension.api.absence.data

import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
class AbsenceDay(
    val date: String,
    override val unsolved: Int,
    override val ok: Int,
    override val missed: Int,
    override val late: Int,
    override val soon: Int,
    override val school: Int
) : AbsenceDataHolder(date.hashCode(), unsolved, ok, missed, late, soon, school),
    Comparable<AbsenceDay> {

    override fun compareTo(other: AbsenceDay): Int {
        return toDate().compareTo(other.toDate())
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

    override fun getLabel(): String {
        return TimeTools.format(toDate(), "d.M.")
    }
}