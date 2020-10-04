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

package cz.lastaapps.bakalariextension.services.timetablenotification

import android.content.Context
import android.text.Html
import android.util.Log
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Hour
import cz.lastaapps.bakalariextension.api.timetable.data.Lesson
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toCzechDate
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toDaySeconds

/**Generates text to timetable notification*/
class NotificationContent(val context: Context) {

    companion object {
        private val TAG = NotificationContent::class.java.simpleName
    }

    //strings in message
    private val nextStr = getString(R.string.timetable_next)
    private val endStr = getString(R.string.timetable_end)
    private val breakStr = getString(R.string.timetable_interruption)
    private val untilStr = getString(R.string.timetable_until)
    private val groupStr = getString(R.string.timetable_group)
    private val freeLessonStr = getString(R.string.timetable_free_lesson)
    private val lastLessonStr = getString(R.string.timetable_last_lesson)

    /** @return map of all available texts
     * Int is representing seconds since midnight
     * for data {100, 200, 300} at time 150s will be texts for 200 returned!*/
    fun generateActions(week: Week): HashMap<Int, Array<CharSequence>?>? {

        Log.i(TAG, "Generating actions")

        //will be returned
        val actions = HashMap<Int, Array<String>?>()

        val day = week.getDay(TimeTools.today.toCzechDate())
        val hours = week.hours
        if (day == null)
            return null

        val firstLesson = day.firstLessonIndex(week.hours)

        //when day ends with lunch, it will be also shown
        val lastLesson =
            (day.lastLessonIndex(week.hours) + (if (day.endsWithLunch(week.hours)) 1 else 0))
                .coerceAtMost(week.hours.size - 1)

        //empty day check - weekend and holidays
        if (firstLesson < 0 || lastLesson < 0)
            return null

        //generates texts
        for (index in firstLesson..lastLesson) {

            val hour = hours[index]
            val lesson = day.getLesson(hour)
            val nextHour = if (index != lastLesson) hours[index + 1] else null
            val nextLesson = if (nextHour != null) day.getLesson(nextHour) else null

            val begin =
                TimeTools.parseTime(hour.begin, TimeTools.TIME_FORMAT, TimeTools.CET).toDaySeconds()

            val end =
                TimeTools.parseTime(hour.end, TimeTools.TIME_FORMAT, TimeTools.CET).toDaySeconds()


            //silent zone before start
            if (index == firstLesson) {
                actions[begin - (60 * 60)] = null
            }

            // @formatter:off
            //normal lesson
            if (day.isNormal(hour)) {

                //lesson is null only during free lesson, line only because of compiler
                if (lesson == null) continue

                when {
                    nextHour == null -> {
                        lessonLast(actions, begin, end, week, hour, lesson)
                    }
                    day.isNormal(nextHour) -> {
                        lessonNextLesson(actions, begin, end, week, hour, nextHour, lesson, nextLesson!!)
                    }
                    day.isFree(nextHour) -> {
                        lessonNextFree(actions, begin, end, week, hour, nextHour, lesson)
                    }
                    day.isAbsence(nextHour) -> {
                        lessonNextAbsence(actions, begin, end, week, hour, nextHour, lesson, nextLesson!!)
                    }
                }
            }
            else if (day.isFree(hour)) {
                //lunch lesson

                when {
                    nextHour == null -> {
                        freeLast(actions, begin, end, week, hour)
                    }
                    day.isNormal(nextHour) -> {
                        freeNextLesson(actions, begin, end, week, hour, nextHour, nextLesson!!)
                    }
                    day.isFree(nextHour) -> {
                        freeNextFree(actions, begin, end, week, hour, nextHour)
                    }
                    day.isAbsence(nextHour) -> {
                        freeNextAbsence(actions, begin, end, week, hour, nextHour, nextLesson!!)
                    }
                }
            }
            else if (day.isAbsence(hour)) {
                //if is class absence

                //lesson is null only during free lesson, line only because of compiler
                if (lesson == null) continue

                when {
                    nextHour == null -> {
                        absenceLast(actions, begin, end, week, hour, lesson)
                    }
                    day.isNormal(nextHour) -> {
                        absenceNextLesson(actions, begin, end, week, hour, nextHour, lesson, nextLesson!!)
                    }
                    day.isFree(nextHour) -> {
                        absenceNextFree(actions, begin, end, week, hour, nextHour, lesson)
                    }
                    day.isAbsence(nextHour) -> {
                        absenceNextAbsence(actions, begin, end, week, hour, nextHour, lesson, nextLesson!!)
                    }
                }
            }
        }

        //converts Strings into CharSequences (Spannable)
        val charSequenceActions = HashMap<Int, Array<CharSequence>?>()
        for (key in actions.keys) {
            val array = actions[key]
            if (array == null) {
                charSequenceActions[key] = null
                continue
            }
            charSequenceActions[key] = arrayOf(
                Html.fromHtml(array[0]) as CharSequence,
                Html.fromHtml(array[1]) as CharSequence
            )
        }

        return charSequenceActions
    }

    private fun lessonLast(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour,
        lesson: Lesson
    ) {
        //the last lesson of the day

        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${week.groups.getByIds(lesson.groupIds)?.shortcut}"
        )

        //during lesson
        actions[end - 10 * 60] = arrayOf(
            "${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${pattern.begin} - ${pattern.end}"
        )

        //last 10 minutes of a lesson
        actions[end] = arrayOf(
            "$lastLessonStr $untilStr ${pattern.end}",
            getString(R.string.timetable_have_nice_day)
        )
    }

    private fun lessonNextLesson(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        lesson: Lesson, nextLesson: Lesson
    ) {
        //before normal lesson

        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${week.groups.getByIds(lesson.groupIds)?.shortcut}"
        )

        //during lesson
        actions[end - 10 * 60] = arrayOf(
            "${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${pattern.begin} - ${pattern.end}, $nextStr: ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}"
        )

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "$nextStr: <b>${week.rooms.getById(nextLesson.roomId)?.shortcut}</b> - ${week.subjects.getById(nextLesson.subjectId)?.name}",
            "$breakStr ${pattern.end} - ${nextPattern.begin}"
        )
    }

    private fun lessonNextFree(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        lesson: Lesson
    ) {
        //before free lesson

        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}"
        )

        //during lesson
        actions[end - 10 * 60] = arrayOf(
            "${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${pattern.begin} - ${pattern.end}, $nextStr: $freeLessonStr"
        )

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}",
            "${pattern.begin} - ${pattern.end}"
        )
    }

    private fun lessonNextAbsence(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        lesson: Lesson, nextLesson: Lesson
    ) {
        //before free lesson

        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${week.groups.getByIds(lesson.groupIds)?.shortcut}"
        )

        //during lesson
        actions[end - 10 * 60] = arrayOf(
            "${"<b>" + week.rooms.getById(lesson.roomId)?.shortcut + "</b>"} - ${week.subjects.getById(lesson.subjectId)?.name}",
            "${pattern.begin} - ${pattern.end}, $nextStr: ${lesson.change?.typeShortcut} ${lesson.change?.typeName}"
        )

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "$nextStr: ${nextLesson.change?.typeShortcut} ${nextLesson.change?.typeName}",
            "${nextPattern.begin} - ${nextPattern.end}"
        )
    }

    private fun freeLast(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour
    ) {
        //the last lesson - free - probably lunch

        //before lesson starts
        //is same as before lesson end
        //actions[begin] = arrayOf("", "")

        //during lesson
        //is same as before lesson end
        //actions[end - 10 * 60] = arrayOf("", "")

        //last 10 minutes of a lesson
        actions[end] = arrayOf(
            lastLessonStr,
            getString(R.string.timetable_finally_home)
        )
    }

    private fun freeNextLesson(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        nextLesson: Lesson
    ) {
        //before normal lesson

        //before lesson starts
        //same as during free lesson
        //actions[begin] = arrayOf("", "")

        //during lesson
        actions[end - 10 * 60] = arrayOf(
            "$freeLessonStr $untilStr ${pattern.end}",
            "$nextStr: ${nextPattern.begin} ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}"
        )

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "$nextStr: <b>${week.rooms.getById(nextLesson.roomId)?.shortcut}</b> - ${week.subjects.getById(nextLesson.subjectId)?.name}",
            "$breakStr ${pattern.end} - ${nextPattern.begin}"
        )
    }

    private fun freeNextFree(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour
    ) {
        //before free lesson

        //before lesson starts
        //same whole time
        //actions[begin] = arrayOf("", "")

        //during lesson
        //same whole time
        //actions[end - 10 * 60] = arrayOf("", "")

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            freeLessonStr,
            "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}"
        )
    }

    private fun freeNextAbsence(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        nextLesson: Lesson
    ) {
        //before free lesson

        //during lesson

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "$freeLessonStr $untilStr ${pattern.end}",
            "$nextStr: ${nextPattern.begin} ${nextLesson.change?.typeShortcut} ${nextLesson.change?.typeName}"
        )
    }

    private fun absenceLast(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour,
        lesson: Lesson
    ) {
        //the last lesson - free - probably lunch

        //before lesson starts
        //is same as before lesson end
        //actions[begin] = arrayOf("", "")
        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${lesson.change?.typeShortcut}",
            "${lesson.change?.typeName}"
        )

        //during lesson
        //is same as before lesson end
        //actions[end - 10 * 60] = arrayOf("", "")

        //last 10 minutes of a lesson
        actions[end] = arrayOf(
            "${lesson.change?.typeShortcut} ${lesson.change?.typeName}",
            "$lastLessonStr ${getString(R.string.timetable_finally_home)}"
        )
    }

    private fun absenceNextLesson(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        lesson: Lesson, nextLesson: Lesson
    ) {
        //before normal lesson

        //before lesson starts
        //same as during free lesson
        //actions[begin] = arrayOf("", "")
        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${lesson.change?.typeShortcut}",
            "${lesson.change?.typeName}"
        )

        //during lesson
        actions[end - 10 * 60] = arrayOf(
            "${lesson.change?.typeShortcut} ${lesson.change?.typeName}",
            "${pattern.begin} -: ${pattern.end}, $nextStr: ${nextPattern.begin} ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}"
        )

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "$nextStr: <b>${week.rooms.getById(nextLesson.roomId)?.shortcut}</b> - ${week.subjects.getById(nextLesson.subjectId)?.name}",
            "$breakStr ${pattern.end} - ${nextPattern.begin}"
        )
    }

    private fun absenceNextFree(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        lesson: Lesson
    ) {
        //before free lesson

        //before lesson starts
        //same whole time
        //actions[begin] = arrayOf("", "")
        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${lesson.change?.typeShortcut}",
            "${lesson.change?.typeName}"
        )

        //during lesson
        //same whole time
        //actions[end - 10 * 60] = arrayOf("", "")

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "${lesson.change?.typeShortcut} ${lesson.change?.typeName}",
            "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}"
        )
    }

    private fun absenceNextAbsence(
        actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
        week: Week, pattern: Hour, nextPattern: Hour,
        lesson: Lesson, nextLesson: Lesson
    ) {
        //before free lesson

        //during lesson
        //before lesson starts
        actions[begin] = arrayOf(
            "${pattern.begin} ${lesson.change?.typeShortcut}",
            "${lesson.change?.typeName}"
        )

        //last 10 minutes of the lesson
        actions[end] = arrayOf(
            "${lesson.change?.typeShortcut} ${lesson.change?.typeName}",
            "$nextStr: ${lesson.change?.typeShortcut} ${lesson.change?.typeName}"
        )
    }
    // @formatter:on


    private fun getString(id: Int): String {
        return context.getString(id)
    }

}