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

package cz.lastaapps.bakalari.api.entity.timetable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.core.DataId
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

/**Stores info about lesson and the change of the lesson*/
@Parcelize
@Entity(tableName = APIBaseKeys.TIMETABLE_LESSON, primaryKeys = ["date", "hourId"])
data class Lesson(
    @ColumnInfo(index = true)
    val date: LocalDate,
    @ColumnInfo(index = true)
    val hourId: Int,
    @Ignore
    val groupIds: ArrayList<String>,
    val subjectId: String,
    val teacherId: String,
    val roomId: String,
    @Ignore
    val cycleIds: ArrayList<String>,
    @Ignore
    val change: Change?,
    @Ignore
    val homeworkIds: ArrayList<String>,
    val theme: String
) : DataId<Int>(hourId) {

    @Deprecated("For Room initialization only")
    constructor(
        date: LocalDate, hourId: Int, subjectId: String,
        teacherId: String, roomId: String, theme: String,
    ) : this(
        date, hourId, ArrayList(), subjectId, teacherId, roomId,
        ArrayList(), null, ArrayList(), theme,
    )

    fun isNormal(): Boolean {
        val change = change ?: return true
        return change.isAdded() || change.isRoomChanged() || change.isUnknown()
    }

    fun isRemoved(): Boolean {
        val change = change ?: return false
        return change.isRemoved() || change.isCanceled()
    }

    fun isAbsence(): Boolean {
        val change = change ?: return false
        return change.isAbsence()
    }

    fun isChanged(): Boolean = change != null

    override fun toString(): String {
        return "{Lesson: $hourId $subjectId $change}"
    }
}