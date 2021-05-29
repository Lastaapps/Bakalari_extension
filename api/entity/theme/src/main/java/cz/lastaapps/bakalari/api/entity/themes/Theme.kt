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

package cz.lastaapps.bakalari.api.entity.themes

import androidx.room.Entity
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.core.DataId
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.tools.TimeTools
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

typealias ThemeList = DataIdList<Theme>

@Parcelize
@Entity(tableName = APIBaseKeys.THEMES, inheritSuperIndices = true)
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