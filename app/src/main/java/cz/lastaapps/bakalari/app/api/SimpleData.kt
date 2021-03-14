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

package cz.lastaapps.bakalari.app.api

import androidx.room.Ignore
//import kotlinx.parcelize.Parcelize
import kotlinx.android.parcel.Parcelize
import java.text.Collator
import java.util.*

/**Parent for classes containing just id, name and shortcut*/
@Parcelize
open class SimpleData(
    @Ignore
    override var id: String,
    open val shortcut: String,
    open val name: String
) : DataId<String>(id), Comparable<SimpleData> {

    override fun toString(): String {
        return if (name != "") name else shortcut
    }

    override fun compareTo(other: SimpleData): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(this.toString(), other.toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleData) return false
        if (!super.equals(other)) return false

        if (id != other.id) return false
        if (shortcut != other.shortcut) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + shortcut.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

}
