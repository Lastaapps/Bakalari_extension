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

package cz.lastaapps.bakalariextension.api

import java.io.Serializable
import java.text.Collator
import java.util.*

/**Parent of the most of the items, the can be then used in DataIDList*/
open class DataId<T>(var id: T) : Serializable {

    override fun equals(other: Any?): Boolean {
        return if (other is DataId<*>)
            id == other.id
        else
            false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**Parent for classes containing just id, name and shortcut*/
open class SimpleData(
    id: String,
    var shortcut: String,
    var name: String
) : DataId<String>(id), Comparable<SimpleData> {

    override fun toString(): String {
        return "{id=$id name=$name shortcut=$shortcut}"
    }

    override fun compareTo(other: SimpleData): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(name, other.name)
    }
}
