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

package cz.lastaapps.bakalari.api.core.timetable.holders

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import cz.lastaapps.bakalari.api.core.SimpleData
import kotlinx.parcelize.Parcelize

@Parcelize
data class Group(
    val classId: String,
    @PrimaryKey
    @ColumnInfo(index = true)
    override var id: String,
    override val shortcut: String,
    override val name: String
) : SimpleData(id, shortcut, name)

