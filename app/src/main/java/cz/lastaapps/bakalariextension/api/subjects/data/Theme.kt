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

import cz.lastaapps.bakalariextension.api.DataID
import cz.lastaapps.bakalariextension.tools.TimeTools

class Theme(
    val date: String,
    val theme: String,
    val note: String,
    val hourCaption: String,
    val lessonLabel: String
) : DataID<String>(lessonLabel), Comparable<Theme> {
    override fun compareTo(other: Theme): Int {
        return lessonLabel.toInt().compareTo(other.lessonLabel.toInt())
    }

    /**formats string to show in views*/
    fun niceDate(): String {
        val parsedDate = TimeTools.parse(date, TimeTools.COMPLETE_FORMAT)
        return TimeTools.format(parsedDate, "d.M.")
    }
}