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

import androidx.room.*
import cz.lastaapps.bakalari.api.entity.core.*
import cz.lastaapps.bakalari.api.entity.homework.Homework


data class HomeworkHolderWithData(
    @Embedded val holder: HomeworkHolder,
    @Relation(
        parentColumn = "class_id",
        entityColumn = "data_id",
    )
    val classData: ClassData,
    @Relation(
        parentColumn = "group_id",
        entityColumn = "data_id",
    )
    val group: GroupData,
    @Relation(
        parentColumn = "subject_id",
        entityColumn = "data_id",
    )
    val subject: SubjectData,
    @Relation(
        parentColumn = "teacher_id",
        entityColumn = "data_id",
    )
    val teacher: TeacherData,
    @Relation(
        parentColumn = "id",
        entityColumn = "data_id",
        associateBy = Junction(HomeworkAttachmentRelation::class)
    )
    val attachments: List<cz.lastaapps.bakalari.api.entity.attachment.Attachment>
) {
    fun toHomework(): Homework = holder.run {
        Homework(
            id,
            dateStart, dateEnd, content, notice,
            done, closed, electronic, finished,
            hour,
            classData.data, group.data, subject.data, teacher.data,
            DataIdList(attachments)
        )
    }
}

@Entity(tableName = APIBaseKeys.HOMEWORK_ATTACHMENTS, primaryKeys = ["id", "data_id"])
data class HomeworkAttachmentRelation(
    @ColumnInfo(index = true, name = "id")
    val homeworkId: String,
    @ColumnInfo(index = true, name = "data_id")
    val attachmentId: String,
)