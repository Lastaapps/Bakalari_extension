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

package cz.lastaapps.bakalari.api.dao.timetable

import androidx.room.*
import cz.lastaapps.bakalari.api.entity.core.*
import cz.lastaapps.bakalari.api.entity.timetable.*
import cz.lastaapps.bakalari.tools.TimeTools.toMonday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

@Dao
abstract class TimetableDao {

    @Transaction
    open suspend fun replaceWeek(
        week: Week,
        from: LocalDate = week.monday,
        to: LocalDate = from.plusDays(4)
    ) {
        delete(from, to)
        insertWeek(week)
    }


    //insert
    @Transaction
    protected open suspend fun insertWeek(week: Week) {
        val days = ArrayList<Day>()
        val lessons = ArrayList<Lesson>()
        val changes = ArrayList<Change>()
        val lessonCycleRelations = ArrayList<LessonCycleRelation>()
        val lessonGroupRelations = ArrayList<LessonGroupRelation>()
        val lessonHomeworkRelations = ArrayList<LessonHomeworkRelation>()

        for (day in week.days) {
            days.add(day)

            for (lesson in day.lessons) {
                lessons.add(lesson)
                lesson.change?.let { changes.add(it) }

                lessonCycleRelations.addAll(lesson.cycleIds.map {
                    LessonCycleRelation(lesson.date, lesson.hourId, it)
                })
                lessonGroupRelations.addAll(lesson.groupIds.map {
                    LessonGroupRelation(lesson.date, lesson.hourId, it)
                })
                lessonHomeworkRelations.addAll(lesson.homeworkIds.map {
                    LessonHomeworkRelation(lesson.date, lesson.hourId, it)
                })
            }
        }


        insertDay(days)
        insertHour(week.hours)
        insertLessons(lessons)
        insertChanges(changes)
        insertTimetableCycleRelation(week.cycles.map { TimetableCycleRelation(week.monday, it.id) })
        insertLessonCycle(lessonCycleRelations)
        insertLessonGroup(lessonGroupRelations)
        insertLessonHomework(lessonHomeworkRelations)

        insertClasses(week.classes.map { ClassData(it) })
        insertClassGroups(week.groups.map { ClassGroupData(it) })
        insertSubjects(week.subjects.map { SubjectData(it) })
        insertTeachers(week.teachers.map { TeacherData(it) })
        insertRooms(week.rooms.map { RoomData(it) })
        insertCycles(week.cycles.map { CycleData(it) })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDay(list: List<Day>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLessons(list: List<Lesson>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertChanges(list: List<Change>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertHour(list: List<Hour>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTimetableCycleRelation(list: List<TimetableCycleRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLessonCycle(list: List<LessonCycleRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLessonGroup(list: List<LessonGroupRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLessonHomework(list: List<LessonHomeworkRelation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertClasses(list: List<ClassData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTeachers(list: List<TeacherData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRooms(list: List<RoomData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSubjects(list: List<SubjectData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertClassGroups(list: List<ClassGroupData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCycles(list: List<CycleData>)


    //obtaining
    @Query("SELECT COUNT(date) FROM ${APIBaseKeys.TIMETABLE_DAY} WHERE date BETWEEN :from AND :to")
    abstract fun getDaysNumber(from: LocalDate, to: LocalDate = from.plusDays(4)): Flow<Int>

    @Query("SELECT date FROM ${APIBaseKeys.TIMETABLE_DAY} ORDER BY date")
    abstract fun getWeeks(): Flow<List<LocalDate>>

    fun getWeek(
        monday: LocalDate,
        friday: LocalDate = monday.plusDays(4)
    ): Flow<Week?> = flow {

        getDays(monday, friday).collect { days ->

            if (days.isEmpty())
                emit(emptyWeek(monday, getHours()))
            else {

                val newDays = ArrayList<Day>()

                for (day in days) {
                    newDays.add(processDay(day))
                }

                emit(createWeek(monday, newDays))
            }
        }
    }.flowOn(Dispatchers.Default)

    fun getWeekForDay(
        date: LocalDate
    ): Flow<Week?> = flow {
        val monday = date.toMonday()

        getDays(date, date).collect { days ->

            if (days.isEmpty())
                emit(emptyWeek(monday, getHours()))
            else {

                val newDays = ArrayList<Day>()

                for (day in days) {
                    newDays.add(processDay(day))
                }

                emit(createWeek(monday, newDays))
            }
        }
    }.flowOn(Dispatchers.Default)

    private fun emptyWeek(monday: LocalDate, hours: List<Hour>): Week =
        Week(
            DataIdList(hours), ArrayList(), DataIdList(), DataIdList(), DataIdList(),
            DataIdList(), DataIdList(), DataIdList(), monday,
        )

    private suspend fun processDay(day: Day): Day {

        val newLessons = DataIdList<Lesson>()

        for (lesson in getLessons(day.date)) {
            val change = getChange(lesson.date, lesson.hourId)

            val cycleIds =
                ArrayList(
                    getLessonsCycles(
                        lesson.date,
                        lesson.hourId
                    ).map { it.otherId })
            val groupIds =
                ArrayList(
                    getLessonsGroups(
                        lesson.date,
                        lesson.hourId
                    ).map { it.otherId })
            val homeworkIds =
                ArrayList(
                    getLessonsHomework(
                        lesson.date,
                        lesson.hourId
                    ).map { it.otherId })

            newLessons.add(
                lesson.copy(
                    change = change,
                    cycleIds = cycleIds,
                    groupIds = groupIds,
                    homeworkIds = homeworkIds
                )
            )
        }

        return day.copy(lessons = newLessons)
    }

    private suspend fun createWeek(monday: LocalDate, days: List<Day>) =
        Week(
            DataIdList(getHours()),
            ArrayList(days),
            DataIdList(getClasses().map { it.data }),
            DataIdList(getClassGroups().map { it.data }),
            DataIdList(getSubjects().map { it.data }),
            DataIdList(getTeachers().map { it.data }),
            DataIdList(getRooms().map { it.data }),
            DataIdList(getCycles(getTimetableCyclesRelations(monday).map { it.dataId }).map { it.data }),
            monday,
        )

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_DAY} WHERE date BETWEEN :from AND :to ORDER BY dayOfWeek ASC")
    protected abstract fun getDays(from: LocalDate, to: LocalDate): Flow<List<Day>>

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_LESSON} WHERE date=:date")
    protected abstract suspend fun getLessons(date: LocalDate): List<Lesson>

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_CHANGE} WHERE date=:date AND hourId=:hourId")
    protected abstract suspend fun getChange(date: LocalDate, hourId: Int): Change?

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_HOUR}")
    protected abstract suspend fun getHours(): List<Hour>

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_CYCLE_RELATION} WHERE id=:date")
    protected abstract suspend fun getTimetableCyclesRelations(date: LocalDate): List<TimetableCycleRelation>

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_LESSON_CYCLE} WHERE date=:date AND hourId=:hourId")
    protected abstract suspend fun getLessonsCycles(
        date: LocalDate,
        hourId: Int
    ): List<LessonCycleRelation>

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_LESSON_GROUP} WHERE date=:date AND hourId=:hourId")
    protected abstract suspend fun getLessonsGroups(
        date: LocalDate,
        hourId: Int
    ): List<LessonGroupRelation>

    @Query("SELECT * FROM ${APIBaseKeys.TIMETABLE_LESSON_HOMEWORK} WHERE date=:date AND hourId=:hourId")
    protected abstract suspend fun getLessonsHomework(
        date: LocalDate,
        hourId: Int
    ): List<LessonHomeworkRelation>

    @Query("SELECT * FROM ${APIBaseKeys.DATA_CLASS}")
    protected abstract suspend fun getClasses(): List<ClassData>

    @Query("SELECT * FROM ${APIBaseKeys.DATA_TEACHER}")
    protected abstract suspend fun getTeachers(): List<TeacherData>

    @Query("SELECT * FROM ${APIBaseKeys.DATA_ROOM}")
    protected abstract suspend fun getRooms(): List<RoomData>

    @Query("SELECT * FROM ${APIBaseKeys.DATA_SUBJECT}")
    protected abstract suspend fun getSubjects(): List<SubjectData>

    @Query("SELECT * FROM ${APIBaseKeys.DATA_CLASS_GROUP}")
    protected abstract suspend fun getClassGroups(): List<ClassGroupData>

    @Query("SELECT * FROM ${APIBaseKeys.DATA_CYCLE} WHERE data_id IN (:ids)")
    protected abstract suspend fun getCycles(ids: List<String>): List<CycleData>


    //deletion
    @Transaction
    open suspend fun delete(from: LocalDate, to: LocalDate = from.plusDays(4)) {
        deleteDays(from, to)
        deleteLessons(from, to)
        deleteChanges(from, to)
        deleteTimetableCycleRelation(from)
        deleteLessonCycle(from, to)
        deleteLessonGroup(from, to)
        deleteLessonHomework(from, to)
    }

    @Query("DELETE FROM ${APIBaseKeys.TIMETABLE_DAY} WHERE date BETWEEN :from AND :to")
    protected abstract suspend fun deleteDays(from: LocalDate, to: LocalDate)

    @Query("DELETE FROM ${APIBaseKeys.TIMETABLE_LESSON} WHERE date BETWEEN :from AND :to")
    protected abstract suspend fun deleteLessons(from: LocalDate, to: LocalDate)

    @Query("DELETE FROM ${APIBaseKeys.TIMETABLE_CHANGE} WHERE date BETWEEN :from AND :to")
    protected abstract suspend fun deleteChanges(from: LocalDate, to: LocalDate)

    @Query("DELETE FROM ${APIBaseKeys.TIMETABLE_CYCLE_RELATION} WHERE id=:date")
    protected abstract suspend fun deleteTimetableCycleRelation(date: LocalDate)

    @Query("DELETE FROM ${APIBaseKeys.TIMETABLE_LESSON_CYCLE} WHERE date BETWEEN :from AND :to")
    protected abstract suspend fun deleteLessonCycle(from: LocalDate, to: LocalDate)

    @Query("DELETE FROM ${APIBaseKeys.TIMETABLE_LESSON_GROUP} WHERE date BETWEEN :from AND :to")
    protected abstract suspend fun deleteLessonGroup(from: LocalDate, to: LocalDate)

    @Query("DELETE FROM ${APIBaseKeys.TIMETABLE_LESSON_HOMEWORK} WHERE date BETWEEN :from AND :to")
    protected abstract suspend fun deleteLessonHomework(from: LocalDate, to: LocalDate)

}