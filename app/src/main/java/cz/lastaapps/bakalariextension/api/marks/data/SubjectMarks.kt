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

package cz.lastaapps.bakalariextension.api.marks.data

import android.os.Parcelable
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import kotlinx.android.parcel.Parcelize
import java.text.Collator
import java.util.*

/**Subject marking info with marks and subject info*/
@Parcelize
class SubjectMarks(
    val marks: DataIdList<Mark>,
    val subject: SimpleData,
    val averageText: String,
    val tempMark: String,
    val subjectNote: String,
    val tempMarkNote: String,
    val pointsOnly: Boolean,
    val predictorEnabled: Boolean
) : Comparable<SubjectMarks>, Parcelable {
    companion object {
        val default: SubjectMarks
            get() = SubjectMarks(
                DataIdList(), SimpleData("", "", ""), "0,00", "", "", "",
                pointsOnly = false,
                predictorEnabled = false
            )
    }

    override fun compareTo(other: SubjectMarks): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(subject.name, other.subject.name)
    }
}