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

package cz.lastaapps.bakalari.api.dao.subject

import androidx.room.*
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.core.TeacherData
import cz.lastaapps.bakalari.api.entity.subjects.Subject
import cz.lastaapps.bakalari.api.entity.subjects.Teacher
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubjectDao {

    //subjects
    @Query("SELECT * FROM ${APIBaseKeys.SUBJECTS} ORDER BY name COLLATE LOCALIZED")
    abstract fun getSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM ${APIBaseKeys.SUBJECTS} WHERE id=:id LIMIT 1")
    abstract suspend fun getSubject(id: String): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSubjects(subjects: List<Subject>)

    @Query("SELECT * FROM ${APIBaseKeys.SUBJECTS} WHERE teacherId=:id")
    abstract suspend fun getTeachersSubjects(id: String): List<Subject>

    @Transaction
    open suspend fun replaceSubjects(subjects: List<Subject>) {
        deleteAll()
        insertSubjects(subjects)
    }

    //teachers
    @Query("SELECT * FROM ${APIBaseKeys.TEACHERS} ORDER BY name COLLATE LOCALIZED")
    abstract fun getTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM ${APIBaseKeys.TEACHERS} WHERE id=:id LIMIT 1")
    abstract suspend fun getTeacher(id: String): Teacher?

    @Query("SELECT * FROM ${APIBaseKeys.DATA_TEACHER} WHERE data_id=:id LIMIT 1")
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

    @Query("DELETE FROM ${APIBaseKeys.SUBJECTS}")
    abstract suspend fun deleteSubjects()

    @Query("DELETE FROM ${APIBaseKeys.TEACHERS}")
    abstract suspend fun deleteTeachers()
}