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

import androidx.room.ColumnInfo
import androidx.room.Entity
import cz.lastaapps.bakalariextension.api.database.APIBase
import java.time.LocalDate

@Entity(tableName = APIBase.TIMETABLE_LESSON_CYCLE, primaryKeys = ["date", "hourId", "otherId"])
data class LessonCycleRelation(
    @ColumnInfo(index = true) val date: LocalDate,
    @ColumnInfo(index = true) val hourId: Int,
    val otherId: String
)

@Entity(tableName = APIBase.TIMETABLE_LESSON_GROUP, primaryKeys = ["date", "hourId", "otherId"])
data class LessonGroupRelation(
    @ColumnInfo(index = true) val date: LocalDate,
    @ColumnInfo(index = true) val hourId: Int,
    val otherId: String
)

@Entity(tableName = APIBase.TIMETABLE_LESSON_HOMEWORK, primaryKeys = ["date", "hourId", "otherId"])
data class LessonHomeworkRelation(
    @ColumnInfo(index = true) val date: LocalDate,
    @ColumnInfo(index = true) val hourId: Int,
    val otherId: String
)

@Entity(primaryKeys = ["id", "data_id"], tableName = APIBase.TIMETABLE_CYCLE_RELATION)
data class TimetableCycleRelation(
    @ColumnInfo(index = true) val id: LocalDate,
    @ColumnInfo(name = "data_id", index = true) val dataId: String
)

