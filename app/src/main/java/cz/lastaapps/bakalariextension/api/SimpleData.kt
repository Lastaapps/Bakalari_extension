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

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.io.Serializable
import java.text.Collator
import java.util.*

/**Parent of the most of the items, the can be then used in DataIDList*/
@Parcelize
open class DataId<T>(open var id: @RawValue T) : Serializable, Parcelable {

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
@Parcelize
open class SimpleData(
    override var id: String,
    open var shortcut: String,
    open var name: String
) : DataId<String>(id), Comparable<SimpleData> {

    override fun toString(): String {
        return if (name != "") name else shortcut
    }

    override fun compareTo(other: SimpleData): Int {
        val collator = Collator.getInstance(Locale.getDefault())

        return collator.compare(this.toString(), other.toString())
    }
}
