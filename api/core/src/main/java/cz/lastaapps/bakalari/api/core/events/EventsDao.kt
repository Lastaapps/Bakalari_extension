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

package cz.lastaapps.bakalari.api.core.events

import androidx.room.*
import cz.lastaapps.bakalari.api.core.database.APIBase
import cz.lastaapps.bakalari.api.core.database.RoomData
import cz.lastaapps.bakalari.api.core.database.StudentData
import cz.lastaapps.bakalari.api.core.database.TeacherData
import cz.lastaapps.bakalari.api.core.events.holders.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EventsDao {

    //general part
    @Transaction
    open suspend fun replaceEvents(list: List<EventHolderWithLists>) {
        deleteAll()
        insert(list)
    }


    //get part
    @Transaction
    @Query("SELECT * FROM ${APIBase.EVENTS} ORDER BY eventStart DESC")
    abstract fun getEvents(): Flow<List<EventHolderWithLists>>

    @Transaction
    @Query("SELECT * FROM ${APIBase.EVENTS} WHERE id=:id LIMIT 1")
    abstract suspend fun getEvent(id: String): EventHolderWithLists?


    //insert part
    suspend fun insert(list: List<EventHolderWithLists>) {
        val eventList = ArrayList<EventHolder>()
        val timesList = ArrayList<EventTime>()
        val classList = ArrayList<EventClassData>()
        val classRelList = ArrayList<EventClassRelation>()
        val teacherList = ArrayList<TeacherData>()
        val teacherRelList = ArrayList<EventTeacherRelation>()
        val roomList = ArrayList<RoomData>()
        val roomRelList = ArrayList<EventRoomRelation>()
        val studentList = ArrayList<StudentData>()
        val studentRelList = ArrayList<EventStudentRelation>()

        for (item in list) {
            eventList.add(item.holder)

            timesList.addAll(item.times)

            classList.addAll(item.classes)
            classRelList.addAll(item.classes.map { EventClassRelation(item.holder.id, it.data.id) })

            teacherList.addAll(item.teachers)
            teacherRelList.addAll(item.teachers.map {
                EventTeacherRelation(
                    item.holder.id,
                    it.data.id
                )
            })

            roomList.addAll(item.rooms)
            roomRelList.addAll(item.rooms.map { EventRoomRelation(item.holder.id, it.data.id) })

            studentList.addAll(item.students)
            studentRelList.addAll(item.students.map {
                EventStudentRelation(
                    item.holder.id,
                    it.data.id
                )
            })

        }

        insertData(
            eventList,
            timesList,
            classList,
            classRelList,
            teacherList,
            teacherRelList,
            roomList,
            roomRelList,
            studentList,
            studentRelList
        )
    }

    /**Divided into two part to not block database for to long*/
    @Transaction
    protected open suspend fun insertData(
        eventList: ArrayList<EventHolder>,
        timesList: ArrayList<EventTime>,
        classList: ArrayList<EventClassData>,
        classRelList: ArrayList<EventClassRelation>,
        teacherList: ArrayList<TeacherData>,
        teacherRelList: ArrayList<EventTeacherRelation>,
        roomList: ArrayList<RoomData>,
        roomRelList: ArrayList<EventRoomRelation>,
        studentList: ArrayList<StudentData>,
        studentRelList: ArrayList<EventStudentRelation>
    ) {
        insertEvents(eventList)
        insertTimes(timesList)
        insertEventClasses(classList)
        insertClassesRelations(classRelList)
        insertTeachers(teacherList)
        insertTeachersRelations(teacherRelList)
        insertRooms(roomList)
        insertRoomsRelations(roomRelList)
        insertStudents(studentList)
        insertStudentsRelations(studentRelList)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertEvents(list: List<EventHolder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTimes(list: List<EventTime>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertEventClasses(list: List<EventClassData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTeachers(list: List<TeacherData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRooms(list: List<RoomData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertStudents(list: List<StudentData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertClassesRelations(list: List<EventClassRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTeachersRelations(list: List<EventTeacherRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRoomsRelations(list: List<EventRoomRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertStudentsRelations(list: List<EventStudentRelation>)


    //Delete part
    @Transaction
    open suspend fun deleteAll() {
        deleteAllEvents()
        deleteAllTimesData()
        deleteAllClassesData()
        deleteAllClassesRelations()
        deleteAllTeachers()
        deleteAllRooms()
        deleteAllStudents()
    }

    @Query("DELETE FROM ${APIBase.EVENTS}")
    protected abstract suspend fun deleteAllEvents()

    @Query("DELETE FROM ${APIBase.EVENTS_TIMES}")
    protected abstract suspend fun deleteAllTimesData()

    @Query("DELETE FROM ${APIBase.EVENTS_CLASSES_DATA}")
    protected abstract suspend fun deleteAllClassesData()

    @Query("DELETE FROM ${APIBase.EVENTS_CLASSES_RELATIONS}")
    protected abstract suspend fun deleteAllClassesRelations()

    @Query("DELETE FROM ${APIBase.EVENTS_TEACHES}")
    protected abstract suspend fun deleteAllTeachers()

    @Query("DELETE FROM ${APIBase.EVENTS_ROOMS}")
    protected abstract suspend fun deleteAllRooms()

    @Query("DELETE FROM ${APIBase.EVENTS_STUDENTS}")
    protected abstract suspend fun deleteAllStudents()
}