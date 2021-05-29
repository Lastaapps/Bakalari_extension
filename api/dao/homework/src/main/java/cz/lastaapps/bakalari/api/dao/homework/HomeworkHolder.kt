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

package cz.lastaapps.bakalari.api.dao.homework

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.homework.Homework
import java.time.ZonedDateTime

@Entity(tableName = APIBaseKeys.HOMEWORK)
data class HomeworkHolder(
    @PrimaryKey
    @ColumnInfo(index = true)
    val id: String,
    val dateStart: ZonedDateTime,
    val dateEnd: ZonedDateTime,
    val content: String,
    val notice: String,
    val done: Boolean,
    val closed: Boolean,
    val electronic: Boolean,
    val finished: Boolean,
    val hour: Int,
    @ColumnInfo(name = "class_id")
    val classId: String,
    @ColumnInfo(name = "group_id")
    val groupId: String,
    @ColumnInfo(name = "subject_id")
    val subjectId: String,
    @ColumnInfo(name = "teacher_id")
    val teacherId: String,
) {
    companion object {
        fun fromHomework(homework: Homework): HomeworkHolder = homework.run {
            HomeworkHolder(
                id,
                dateStart, dateEnd, content, notice,
                done, closed, electronic, finished,
                hour,
                classInfo.id, group.id, subject.id, teacher.id
            )
        }
    }
}

