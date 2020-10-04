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

package cz.lastaapps.bakalariextension.api.subjects

import androidx.room.*
import cz.lastaapps.bakalariextension.api.database.APIBase
import cz.lastaapps.bakalariextension.api.database.TeacherData
import cz.lastaapps.bakalariextension.api.subjects.data.Subject
import cz.lastaapps.bakalariextension.api.subjects.data.Teacher
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubjectDao {

    //subjects
    @Query("SELECT * FROM ${APIBase.SUBJECTS} ORDER BY name COLLATE LOCALIZED")
    abstract fun getSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM ${APIBase.SUBJECTS} WHERE id=:id LIMIT 1")
    abstract suspend fun getSubject(id: String): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSubjects(subjects: List<Subject>)

    @Query("SELECT * FROM ${APIBase.SUBJECTS} WHERE teacherId=:id")
    abstract suspend fun getTeachersSubjects(id: String): List<Subject>

    @Transaction
    open suspend fun replaceSubjects(subjects: List<Subject>) {
        deleteAll()
        insertSubjects(subjects)
    }

    //teachers
    @Query("SELECT * FROM ${APIBase.TEACHERS} ORDER BY name COLLATE LOCALIZED")
    abstract fun getTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM ${APIBase.TEACHERS} WHERE id=:id LIMIT 1")
    abstract suspend fun getTeacher(id: String): Teacher?

    @Query("SELECT * FROM ${APIBase.DATA_TEACHER} WHERE data_id=:id LIMIT 1")
    abstract suspend fun getSimpleTeacher(id: String): TeacherData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTeachers(teachers: List<Teacher>)

    @Transaction
    open suspend fun replaceTeachers(teachers: List<Teacher>) {
        deleteAll()
        insertTeachers(teachers)
    }

    //deletion
    @Transaction
    open suspend fun deleteAll() {
        deleteSubjects()
        deleteTeachers()
    }

    @Query("DELETE FROM ${APIBase.SUBJECTS}")
    abstract suspend fun deleteSubjects()

    @Query("DELETE FROM ${APIBase.TEACHERS}")
    abstract suspend fun deleteTeachers()
}