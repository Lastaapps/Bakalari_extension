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

package cz.lastaapps.bakalari.app.api.marks.data

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import cz.lastaapps.bakalari.app.api.SimpleData
import cz.lastaapps.bakalari.app.api.database.APIBase
import kotlinx.android.parcel.Parcelize
import java.text.Collator
import java.util.*

/**Subject marking info with marks and subject info*/
@Parcelize
@Entity(
    tableName = APIBase.MARK_SUBJECT,
    primaryKeys = ["id"],
    indices = [Index(value = ["id", "name"])]
)
data class MarksSubject(
    @Embedded
    val subject: SimpleData,
    val averageText: String,
    val tempMark: String,
    val subjectNote: String,
    val tempMarkNote: String,
    val pointsOnly: Boolean,
    val predictorEnabled: Boolean
) : Comparable<MarksSubject>, Parcelable {
    companion object {
        val default: MarksSubject
            get() = MarksSubject(
                SimpleData("", "", ""), "0,00", "", "", "",
                pointsOnly = false,
                predictorEnabled = false
            )
    }

    override fun compareTo(other: MarksSubject): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(subject.name, other.subject.name)
    }
}