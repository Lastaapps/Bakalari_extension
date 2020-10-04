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

package cz.lastaapps.bakalariextension.api.database
/*
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GeneralDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertClasses(list: List<ClassData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTeachers(list: List<TeacherData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRooms(list: List<RoomData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertStudents(list: List<StudentData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSubjects(list: List<SubjectData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertGroup(list: List<GroupData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertClassGroups(list: List<ClassGroupData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCycles(list: List<CycleData>)


    @Query("SELECT * FROM ${APIBase.DATA_CLASS}")
    protected abstract suspend fun getClasses(): List<ClassData>

    @Query("SELECT * FROM ${APIBase.DATA_TEACHER}")
    protected abstract suspend fun getTeachers(): List<TeacherData>

    @Query("SELECT * FROM ${APIBase.DATA_ROOM}")
    protected abstract suspend fun getRooms(): List<RoomData>

    @Query("SELECT * FROM ${APIBase.DATA_STUDENT}")
    protected abstract suspend fun getStudents(): List<StudentData>

    @Query("SELECT * FROM ${APIBase.DATA_SUBJECT}")
    protected abstract suspend fun getSubjects(): List<SubjectData>

    @Query("SELECT * FROM ${APIBase.DATA_GROUP}")
    protected abstract suspend fun getGroup(): List<GroupData>

    @Query("SELECT * FROM ${APIBase.DATA_CLASS_GROUP}")
    protected abstract suspend fun getClassGroups(): List<ClassGroupData>

    @Query("SELECT * FROM ${APIBase.DATA_CYCLE}")
    protected abstract suspend fun getCycles(): List<CycleData>


    @Query("SELECT * FROM ${APIBase.DATA_CLASS}")
    protected abstract fun getClassesFlow(): Flow<List<ClassData>>

    @Query("SELECT * FROM ${APIBase.DATA_TEACHER}")
    protected abstract fun getTeachersFlow(): Flow<List<TeacherData>>

    @Query("SELECT * FROM ${APIBase.DATA_ROOM}")
    protected abstract fun getRoomsFlow(): Flow<List<RoomData>>

    @Query("SELECT * FROM ${APIBase.DATA_STUDENT}")
    protected abstract fun getStudentsFlow(): Flow<List<StudentData>>

    @Query("SELECT * FROM ${APIBase.DATA_SUBJECT}")
    protected abstract fun getSubjectsFlow(): Flow<List<SubjectData>>

    @Query("SELECT * FROM ${APIBase.DATA_GROUP}")
    protected abstract fun getGroupFlow(): Flow<List<GroupData>>

    @Query("SELECT * FROM ${APIBase.DATA_CLASS_GROUP}")
    protected abstract fun getClassGroupsFlow(): Flow<List<ClassGroupData>>

    @Query("SELECT * FROM ${APIBase.DATA_CYCLE}")
    protected abstract fun getCyclesFlow(): Flow<List<CycleData>>

}
*/