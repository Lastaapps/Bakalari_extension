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

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

/**Represents change in timetable*/
@Parcelize
data class Change(
    var subject: String,
    var day: String,
    var hours: String,
    var changeType: String,
    var description: String,
    var time: String,
    var typeShortcut: String,
    var typeName: String
) : Serializable, Parcelable {
    /**Absence, normal lesson is canceled because of this*/
    fun isCanceled(): Boolean {
        return changeType == CANCELED
    }

    /**New lesson in timetable*/
    fun isAdded(): Boolean {
        return changeType == ADDED
    }

    /**Empty spot in timetable*/
    fun isRemoved(): Boolean {
        return changeType == REMOVED
    }

    override fun toString(): String {
        return "{Change: $changeType $subject $typeShortcut $description}"
    }

    companion object {
        const val ADDED = "Added"
        const val REMOVED = "Removed"
        const val CANCELED = "Canceled"
    }
}