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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import cz.lastaapps.bakalariextension.api.DataId
import cz.lastaapps.bakalariextension.api.database.APIBase
import kotlinx.android.parcel.Parcelize
import java.time.LocalDate

/**Stores info about lesson and the change of the lesson*/
@Parcelize
@Entity(tableName = APIBase.TIMETABLE_LESSON, primaryKeys = ["date", "hourId"])
data class Lesson(
    @ColumnInfo(index = true)
    var date: LocalDate,
    @ColumnInfo(index = true)
    var hourId: Int,
    @Ignore
    var groupIds: ArrayList<String>,
    var subjectId: String,
    var teacherId: String,
    var roomId: String,
    @Ignore
    var cycleIds: ArrayList<String>,
    @Ignore
    var change: Change?,
    @Ignore
    var homeworkIds: ArrayList<String>,
    var theme: String
) : DataId<Int>(hourId) {

    @Deprecated("For Room initialization only")
    constructor(
        date: LocalDate,
        hourId: Int,
        subjectId: String,
        teacherId: String,
        roomId: String,
        theme: String
    ) : this(
        date,
        hourId,
        ArrayList(),
        subjectId,
        teacherId,
        roomId,
        ArrayList(),
        null,
        ArrayList(),
        theme
    )

    fun isNormal(): Boolean {
        if (change == null) return true
        if (change!!.isAdded()) return true
        return false
    }

    fun isRemoved(): Boolean {
        if (change == null) return false
        if (change!!.isRemoved() || change!!.isCanceled()) return true
        return false
    }

    fun isAbsence(): Boolean {
        if (change == null) return false
        if (change!!.isAbsence()) return true
        return false
    }

    fun isChanged(): Boolean {
        return change != null
    }

    override fun toString(): String {
        return "{Lesson: $hourId $subjectId $change}"
    }
}