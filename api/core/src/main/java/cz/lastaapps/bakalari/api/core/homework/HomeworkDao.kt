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

package cz.lastaapps.bakalari.api.core.homework

import androidx.room.*
import cz.lastaapps.bakalari.api.core.SimpleData
import cz.lastaapps.bakalari.api.core.attachment.holders.Attachment
import cz.lastaapps.bakalari.api.core.database.*
import cz.lastaapps.bakalari.api.core.homework.holders.Homework
import cz.lastaapps.bakalari.api.core.homework.holders.HomeworkAttachmentRelation
import cz.lastaapps.bakalari.api.core.homework.holders.HomeworkHolder
import cz.lastaapps.bakalari.api.core.homework.holders.HomeworkHolderWithData
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

@Dao
abstract class HomeworkDao {

    companion object {
        private const val table = APIBase.HOMEWORK
    }

    //general
    @Transaction
    open suspend fun replaceData(list: List<Homework>) {
        deleteAll()
        insert(list)
    }

    //get
    @Transaction
    @Query("SELECT * FROM $table ORDER BY dateEnd DESC, dateStart DESC")
    abstract fun getHomeworkList(): Flow<List<HomeworkHolderWithData>>

    @Transaction
    @Query("SELECT * FROM $table WHERE dateEnd >= :date OR done == 0 ORDER BY dateEnd DESC, dateStart DESC")
    abstract fun getCurrentHomeworkList(date: ZonedDateTime): Flow<List<HomeworkHolderWithData>>

    @Transaction
    @Query("SELECT * FROM $table WHERE dateEnd < :date AND done != 0 ORDER BY dateEnd DESC, dateStart DESC")
    abstract fun getOldHomeworkList(date: ZonedDateTime): Flow<List<HomeworkHolderWithData>>

    @Transaction
    @Query("SELECT * FROM $table WHERE dateStart <= :end AND dateEnd >= :start ORDER BY dateEnd DESC, dateStart DESC")
    abstract fun getHomeworkListForDates(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): Flow<List<HomeworkHolderWithData>>

    @Transaction
    @Query("SELECT * FROM $table WHERE subject_id=:subjectId ORDER BY dateEnd DESC, dateStart DESC")
    abstract suspend fun getHomeworkListForSubject(subjectId: String): List<HomeworkHolderWithData>


    @Transaction
    @Query("SELECT * FROM $table WHERE id=:id LIMIT 1")
    abstract suspend fun getHomework(id: String): HomeworkHolderWithData?

    @Query("SELECT id FROM $table WHERE dateEnd >= :date OR done == 0 ORDER BY dateEnd DESC, dateStart DESC")
    abstract suspend fun getCurrentIds(date: ZonedDateTime): List<String>

    //insertion
    suspend fun insert(list: List<Homework>) {
        val holderList = ArrayList<HomeworkHolder>()
        val classList = HashSet<SimpleData>()
        val groupList = HashSet<SimpleData>()
        val subjectList = HashSet<SimpleData>()
        val teacherList = HashSet<SimpleData>()
        val attachmentList = ArrayList<Attachment>()
        val attachmentRelList = ArrayList<HomeworkAttachmentRelation>()

        for (homework in list) {
            holderList.add(HomeworkHolder.fromHomework(homework))

            classList.add(homework.classInfo)
            groupList.add(homework.group)
            subjectList.add(homework.subject)
            teacherList.add(homework.teacher)
            attachmentList.addAll(homework.attachments)

            for (attachment in homework.attachments) {
                attachmentRelList.add(HomeworkAttachmentRelation(homework.id, attachment.id))
            }
        }

        insertData(
            holderList,
            classList,
            groupList,
            subjectList,
            teacherList,
            attachmentList,
            attachmentRelList
        )
    }

    @Transaction
    protected open suspend fun insertData(
        holderList: ArrayList<HomeworkHolder>,
        classList: HashSet<SimpleData>,
        groupList: HashSet<SimpleData>,
        subjectList: HashSet<SimpleData>,
        teacherList: HashSet<SimpleData>,
        attachmentList: ArrayList<Attachment>,
        attachmentRelList: ArrayList<HomeworkAttachmentRelation>
    ) {
        insertHolder(holderList)
        insertClass(classList.map { ClassData(it) })
        insertGroup(groupList.map { GroupData(it) })
        insertSubject(subjectList.map { SubjectData(it) })
        insertTeacher(teacherList.map { TeacherData(it) })
        insertAttachments(attachmentList)
        insertAttachmentRelations(attachmentRelList)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertHolder(data: List<HomeworkHolder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertClass(data: List<ClassData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertGroup(data: List<GroupData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSubject(data: List<SubjectData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTeacher(data: List<TeacherData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAttachments(list: List<Attachment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAttachmentRelations(list: List<HomeworkAttachmentRelation>)


    //deletion
    @Transaction
    open suspend fun deleteAll() {
        deleteHolders()
        deleteAttachmentRelations()
    }

    @Query("DELETE FROM $table")
    protected abstract suspend fun deleteHolders()

    @Query("DELETE FROM ${APIBase.HOMEWORK_ATTACHMENTS}")
    protected abstract suspend fun deleteAttachmentRelations()
}