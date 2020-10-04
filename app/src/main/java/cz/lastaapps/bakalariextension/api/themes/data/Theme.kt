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

package cz.lastaapps.bakalariextension.api.themes.data

import androidx.room.Entity
import cz.lastaapps.bakalariextension.api.DataId
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.database.APIBase
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

typealias ThemeList = DataIdList<Theme>

@Parcelize
@Entity(tableName = APIBase.THEMES, inheritSuperIndices = true)
data class Theme(
    val subjectId: String,
    val date: ZonedDateTime,
    val theme: String,
    val note: String,
    val hourCaption: String,
    val lessonLabel: String,
) : DataId<String>(themeId(subjectId, lessonLabel)), Comparable<Theme> {

    override fun compareTo(other: Theme): Int {
        return -1 * lessonLabel.toInt().compareTo(other.lessonLabel.toInt())
    }

    /**formats string to show in views*/
    fun niceDate(): String {
        return TimeTools.format(date, "d.M.")
    }

    companion object {
        fun themeId(theme: Theme) = themeId(theme.subjectId, theme.lessonLabel)
        fun themeId(subjectId: String, lessonLabel: String) = "${subjectId}_${lessonLabel}"
    }
}