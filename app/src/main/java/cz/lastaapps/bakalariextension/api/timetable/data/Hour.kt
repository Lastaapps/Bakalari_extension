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

/**Stores lesson start and end times + id which is used to find lessons in day*/
class Hour (
    id: Int,
    var caption: String,
    var begin: String,
    var end: String
): Comparable<Hour>, DataID<Int>(id) {
    override fun compareTo(other: Hour): Int {
        return caption.toInt().compareTo(other.caption.toInt())
    }
}