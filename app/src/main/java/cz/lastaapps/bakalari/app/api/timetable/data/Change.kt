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

package cz.lastaapps.bakalari.app.api.timetable.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.api.database.APIBase
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.time.LocalDate

/**Represents change in timetable*/
@Parcelize
@Entity(tableName = APIBase.TIMETABLE_CHANGE, primaryKeys = ["date", "hourId"])
data class Change(
    val subject: String,
    @ColumnInfo(index = true)
    val date: LocalDate,
    @ColumnInfo(index = true)
    val hourId: Int,
    val hours: String,
    val changeType: String,
    val description: String,
    val time: String,
    val typeShortcut: String,
    val typeName: String
) : Serializable, Parcelable {

    /**Canceled lesson, not moved ones*/
    fun isCanceled(): Boolean = changeType == CANCELED && typeName == ""

    /**Absence lessons, marked as canceled*/
    fun isAbsence(): Boolean = changeType == CANCELED && typeName != ""

    /**New lesson in timetable*/
    fun isAdded(): Boolean = changeType == ADDED

    /**Empty spot in timetable, moved lesson*/
    fun isRemoved(): Boolean = changeType == REMOVED

    /**Room changed*/
    fun isRoomChanged(): Boolean = changeType == ROOM_CHANGED

    /**if error is not one of the known ones*/
    //TODO report
    fun isUnknown(): Boolean = changeType !in allChangeTypes

    override fun toString(): String {
        return "{Change: $changeType $subject $typeShortcut $description}"
    }

    companion object {
        const val ADDED = "Added"
        const val REMOVED = "Removed"
        const val CANCELED = "Canceled"
        const val ROOM_CHANGED = "RoomChanged"
        val allChangeTypes = listOf(ADDED, REMOVED, CANCELED, ROOM_CHANGED)

        fun Change?.getStringId(): Int? {
            return when (this?.changeType) {
                null -> return null
                ADDED -> R.string.timetable_change_added
                REMOVED -> R.string.timetable_change_removed
                CANCELED -> R.string.timetable_change_canceled
                ROOM_CHANGED -> R.string.timetable_change_room_changed
                else -> R.string.timetable_change_unknown
            }
        }
    }
}