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

package cz.lastaapps.bakalari.api.entity.core

object APIBaseKeys {

    const val DATABASE_NAME = "API_DATABASE"

    //table names
    const val ABSENCE_THRESHOLD = "absence_threshold"
    const val ABSENCE_DAY = "absence_day"
    const val ABSENCE_SUBJECT = "absence_subject"

    const val ATTACHMENT = "attachments"

    const val EVENTS = "events"
    const val EVENTS_TIMES = "events_times"
    const val EVENTS_CLASSES_DATA = "events_classes"
    const val EVENTS_CLASSES_RELATIONS = "rel_events_classes"
    const val EVENTS_TEACHES = "rel_events_teachers"
    const val EVENTS_ROOMS = "rel_events_rooms"
    const val EVENTS_STUDENTS = "rel_events_students"

    const val HOMEWORK = "homework"
    const val HOMEWORK_ATTACHMENTS = "rel_homework_attachments"

    const val MARK_SUBJECT = "mark_subjects"
    const val MARKS = "marks"

    const val SUBJECTS = "subjects"
    const val TEACHERS = "teachers"
    const val THEMES = "themes"

    const val TIMETABLE_CHANGE = "timetable_changes"
    const val TIMETABLE_DAY = "timetable_days"
    const val TIMETABLE_HOUR = "timetable_hours"
    const val TIMETABLE_LESSON = "timetable_lessons"
    const val TIMETABLE_CYCLE_RELATION = "rel_timetable_cycle"
    const val TIMETABLE_LESSON_CYCLE = "rel_timetable_lessons_cycle"
    const val TIMETABLE_LESSON_GROUP = "rel_timetable_lessons_group"
    const val TIMETABLE_LESSON_HOMEWORK = "rel_timetable_lessons_homework"

    const val USER = "user"
    const val USER_MODULE = "user_modules"

    const val JSON_STORAGE = "json_storage"

    const val DATA_CLASS = "data_classes"
    const val DATA_TEACHER = "data_teachers"
    const val DATA_ROOM = "data_rooms"
    const val DATA_STUDENT = "data_students"
    const val DATA_SUBJECT = "data_subject"
    const val DATA_GROUP = "data_groups"
    const val DATA_CLASS_GROUP = "data_class_groups"
    const val DATA_CYCLE = "data_cycles"
}