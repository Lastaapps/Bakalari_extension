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

import java.io.Serializable

/**Parent of the most of the items, the can be then used in DataIDList*/
open class DataID<T>(var id: T): Serializable

/**Parent for classes containing just id, name and shortcut*/
open class TTData(
    id: String,
    var shortcut: String,
    var name: String
): DataID<String>(id) {

    override fun equals(other: Any?): Boolean {
        return if (other is TTData)
            id == other.id
        else
            false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "{id=$id name=$name shortcut=$shortcut}"
    }

    class Room(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Teacher(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Subject(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Group(var classId: String, id: String, shortcut: String, name: String):
        TTData(id, shortcut, name) {}
    class Class(id: String, shortcut: String, name: String) : TTData(id, shortcut, name) {}
    class Cycle(id: String, shortcut: String, name: String) :
        TTData(id, shortcut, name), Comparable<Cycle> {
        override fun compareTo(other: Cycle): Int {
            return id.compareTo(other.id)
        }

    }
}