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

package cz.lastaapps.bakalari.api.entity.absence

import androidx.room.Entity
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.tools.TimeTools
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
@Entity(tableName = APIBaseKeys.ABSENCE_DAY, inheritSuperIndices = true)
data class AbsenceDay(
    val date: ZonedDateTime,
    override val unsolved: Int,
    override val ok: Int,
    override val missed: Int,
    override val late: Int,
    override val soon: Int,
    override val school: Int
) : AbsenceWrapper(date.toInstant().epochSecond, unsolved, ok, missed, late, soon, school),
    Comparable<AbsenceDay> {

    override fun compareTo(other: AbsenceDay): Int {
        return date.compareTo(other.date)
    }

    override fun getLabel(): String {
        return TimeTools.format(date, "d.M.")
    }
}