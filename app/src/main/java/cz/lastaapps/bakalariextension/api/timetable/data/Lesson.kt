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

import cz.lastaapps.bakalariextension.api.DataID

/**Stores info about lesson and the change of the lesson*/
class Lesson(
    hourId: Int,
    var groupIds: ArrayList<String>,
    var subjectId: String,
    var teacherId: String,
    var roomId: String,
    var cycleIds: ArrayList<String>,
    var change: Change?,
    var homeworkIds: ArrayList<String>,
    var theme: String
): DataID<Int>(hourId) {

    fun isNormal(): Boolean {
        if (change == null) return true
        if (change!!.isAdded()) return true
        return false
    }

    fun isRemoved(): Boolean {
        if (change == null) return false
        if (change!!.isRemoved()) return true
        return false
    }

    fun isAbsence(): Boolean {
        if (change == null) return false
        if (change!!.isCanceled()) return true
        return false
    }

    fun isChanged(): Boolean {
        return change != null
    }

    override fun toString(): String {
        return "{Lesson: $id $subjectId $change}"
    }
}