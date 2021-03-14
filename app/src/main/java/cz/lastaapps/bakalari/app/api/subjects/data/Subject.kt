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

package cz.lastaapps.bakalari.app.api.subjects.data

import androidx.room.Entity
import androidx.room.Ignore
import cz.lastaapps.bakalari.app.api.DataId
import cz.lastaapps.bakalari.app.api.DataIdList
import cz.lastaapps.bakalari.app.api.database.APIBase
import kotlinx.android.parcel.Parcelize
import java.text.Collator
import java.util.*

typealias SubjectList = DataIdList<Subject>
typealias TeacherList = DataIdList<Teacher>
typealias SubjectTeacherLists = Pair<SubjectList, TeacherList>

@Parcelize
@Entity(tableName = APIBase.SUBJECTS, inheritSuperIndices = true)
data class Subject(
    @Ignore override var id: String,
    val name: String,
    val shortcut: String,
    val teacherId: String,
) : DataId<String>(id), Comparable<Subject> {

    override fun compareTo(other: Subject): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(name, other.name)
    }
}
