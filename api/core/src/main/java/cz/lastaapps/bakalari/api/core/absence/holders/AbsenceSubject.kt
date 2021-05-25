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

package cz.lastaapps.bakalari.api.core.absence.holders

import androidx.room.Entity
import androidx.room.Ignore
import cz.lastaapps.bakalari.api.core.DataId
import cz.lastaapps.bakalari.api.core.database.APIBase
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.text.Collator
import java.util.*

@Parcelize
@Entity(tableName = APIBase.ABSENCE_SUBJECT, inheritSuperIndices = true)
data class AbsenceSubject(
    val name: String,
    val lessonCount: Int,
    val base: Int,
    val late: Int,
    val soon: Int,
    val school: Int
) : DataId<String>(name), Comparable<AbsenceSubject> {

    override fun compareTo(other: AbsenceSubject): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(name, other.name)
    }

    @delegate:Ignore
    @IgnoredOnParcel
    val percents: String by lazy {
        val percents = 100.0 * base / lessonCount
        String.format(Locale.UK, "%.2f", percents) + "%"
    }

    fun thresholdReached(threshold: Double): Boolean {
        return threshold <= (1.0 * base / lessonCount)
    }
}