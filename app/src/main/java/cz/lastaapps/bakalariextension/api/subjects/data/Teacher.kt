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

package cz.lastaapps.bakalariextension.api.subjects.data

import androidx.room.Entity
import androidx.room.Ignore
import cz.lastaapps.bakalariextension.api.DataId
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.database.APIBase
import kotlinx.android.parcel.Parcelize
import java.text.Collator
import java.util.*

@Parcelize
@Entity(tableName = APIBase.TEACHERS, inheritSuperIndices = true)
data class Teacher(
    @Ignore
    override var id: String,
    val name: String,
    val shortcut: String,
    val email: String,
    val web: String,
    val phoneSchool: String,
    val phoneHome: String,
    val phoneMobile: String,
) : DataId<String>(id), Comparable<Teacher> {

    override fun compareTo(other: Teacher): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(name, other.name)
    }

    fun toSimpleData() = SimpleData(id, shortcut, name)
}