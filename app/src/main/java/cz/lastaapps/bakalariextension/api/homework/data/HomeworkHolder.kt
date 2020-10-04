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

package cz.lastaapps.bakalariextension.api.homework.data

import androidx.room.*
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.attachment.data.Attachment
import cz.lastaapps.bakalariextension.api.database.*
import java.time.ZonedDateTime

@Entity(tableName = APIBase.HOMEWORK)
data class HomeworkHolder(
    @PrimaryKey
    @ColumnInfo(index = true)
    var id: String,
    var dateAward: ZonedDateTime,
    var dateControl: ZonedDateTime?,
    var dateDone: ZonedDateTime,
    var dateStart: ZonedDateTime,
    var dateEnd: ZonedDateTime,
    var content: String,
    var notice: String,
    var done: Boolean,
    var closed: Boolean,
    var electronic: Boolean,
    var hour: Int,
    @ColumnInfo(name = "class_id")
    var classId: String,
    @ColumnInfo(name = "group_id")
    var groupId: String,
    @ColumnInfo(name = "subject_id")
    var subjectId: String,
    @ColumnInfo(name = "teacher_id")
    var teacherId: String,
) {
    companion object {
        fun fromHomework(homework: Homework): HomeworkHolder = homework.run {
            HomeworkHolder(
                id,
                dateAward,
                dateControl,
                dateDone,
                dateStart,
                dateEnd,
                content,
                notice,
                done,
                closed,
                electronic,
                hour,
                classInfo.id,
                group.id,
                subject.id,
                teacher.id
            )
        }
    }
}

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
    val attachments: List<Attachment>
) {
    fun toHomework(): Homework = holder.run {
        Homework(
            id,
            dateAward,
            dateControl,
            dateDone,
            dateStart,
            dateEnd,
            content,
            notice,
            done,
            closed,
            electronic,
            hour,
            classData.data,
            group.data,
            subject.data,
            teacher.data,
            DataIdList(attachments)
        )
    }
}

@Entity(tableName = APIBase.HOMEWORK_ATTACHMENTS, primaryKeys = ["id", "data_id"])
data class HomeworkAttachmentRelation(
    @ColumnInfo(index = true, name = "id")
    val homeworkId: String,
    @ColumnInfo(index = true, name = "data_id")
    val attachmentId: String,
)
