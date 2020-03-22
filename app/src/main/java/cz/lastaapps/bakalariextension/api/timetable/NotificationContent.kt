package cz.lastaapps.bakalariextension.api.timetable

import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Hour
import cz.lastaapps.bakalariextension.api.timetable.data.Lesson
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App
import cz.lastaapps.bakalariextension.tools.TimeTools

/**Generates text to timetable notification*/
class NotificationContent {

    companion object {

        private val nextStr = getString(R.string.next)
        private val endStr = getString(R.string.end)
        private val breakStr = getString(R.string.interuption)
        private val untilStr = getString(R.string.until)
        private val groupStr = getString(R.string.group)
        private val freeLessonStr = getString(R.string.free_lesson)

        fun generateActions(week: Week): HashMap<Int, Array<String>?>? {
            val actions = HashMap<Int, Array<String>?>()

            val day = week.getDay(TimeTools.cal)
            val patterns = week.hours
            if (day == null)
                return null

            val firstLesson = day.firstLessonIndex(week.hours)
            val lastLesson =
                (day.lastLessonIndex(week.hours) + if (day.endsWithLunch(week.hours)) 1 else 0)
                    .coerceAtMost(day.lessons.size - 1)

            if (firstLesson < 0 || lastLesson < 0)
                return null

            for (index in firstLesson..lastLesson) {

                val hour = patterns[index]
                val lesson = day.getLesson(hour) ?: continue
                val nextHour = if (index != lastLesson) patterns[index + 1] else null
                val nextLesson = if (nextHour != null) day.getLesson(nextHour) else null

                val begin = TimeTools.calToSeconds(
                    TimeTools.parseTime(hour.begin, TimeTools.TIME_FORMAT, TimeTools.CET)
                )
                val end = TimeTools.calToSeconds(
                    TimeTools.parseTime(hour.end, TimeTools.TIME_FORMAT, TimeTools.CET)
                )

                //silent zone before start
                if (index == firstLesson) {
                    actions[begin - (60 * 60)] = null
                }

                // @formatter:off
                //normal lesson
                if (day.isNormal(hour)) {

                    if (nextLesson == null || nextHour == null) {
                        lessonLast(actions, begin, end, week, hour, lesson)
                    }
                    else if (day.isNormal(nextHour)) {
                        lessonNextLesson(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                    else if (day.isFree(nextHour)) {
                        lessonNextFree(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                    else if (day.isAbsence(nextHour)) {
                        lessonNextAbsence(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                }
                else if (day.isFree(hour)) {
                    //lunch lesson

                    if (nextLesson == null || nextHour == null) {
                        freeLast(actions, begin, end, week, hour, lesson)
                    }
                    else if (day.isNormal(nextHour)) {
                        freeNextLesson(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                    else if (day.isFree(nextHour)) {
                        freeNextFree(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                    else if (day.isAbsence(nextHour)) {
                        freeNextAbsence(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                }
                else if (day.isAbsence(hour)) {
                    //if is class absence

                    if (nextLesson == null || nextHour == null) {
                        absenceLast(actions, begin, end, week, hour, lesson)
                    }
                    else if (day.isNormal(nextHour)) {
                        absenceNextLesson(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                    else if (day.isFree(nextHour)) {
                        absenceNextFree(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                    else if (day.isAbsence(nextHour)) {
                        absenceNextAbsence(actions, begin, end, week, hour, nextHour, lesson, nextLesson)
                    }
                }
                // @formatter:on
            }
            return actions
        }

        private fun lessonLast(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            week: Week, pattern: Hour,
            lesson: Lesson
        ) {
            //the last lesson of the day

            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${week.groups.getById(lesson.groupIds)?.shortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "$endStr ${pattern.end}"
            )

            //last 10 minutes of a lesson
            actions[end] = arrayOf(
                "$endStr ${pattern.end}",
                getString(R.string.have_nice_day)
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
                "${pattern.begin} ${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${week.groups.getById(lesson.groupIds)?.shortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "$endStr ${pattern.end}, $nextStr: ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}",
                "$breakStr ${pattern.end} - ${nextPattern.begin}"
            )
        }

        private fun lessonNextFree(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            week: Week, pattern: Hour, nextPattern: Hour,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before free lesson

            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${week.groups.getById(lesson.groupIds)?.shortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "$endStr ${pattern.end}, $nextStr: $freeLessonStr"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}",
                "$endStr ${pattern.end}"
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
                "${pattern.begin} ${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "${week.teachers.getById(lesson.teacherId)?.name}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${week.groups.getById(lesson.groupIds)?.shortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${week.rooms.getById(lesson.roomId)?.shortcut} - ${week.subjects.getById(lesson.subjectId)?.name}",
                "$endStr ${pattern.end}, $nextStr: ${lesson.change?.typeShortcut} ${lesson.change?.typeName}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: ${lesson.change?.typeShortcut} ${lesson.change?.typeName}",
                "$endStr ${pattern.end}"
            )
        }

        private fun freeLast(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            week: Week, pattern: Hour,
            lesson: Lesson
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
                "$freeLessonStr",
                "${getString(R.string.finally_home)}"
            )
        }

        private fun freeNextLesson(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            week: Week, pattern: Hour, nextPattern: Hour,
            lesson: Lesson, nextLesson: Lesson
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
                "$nextStr: ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}",
                "$breakStr ${pattern.end} - ${nextPattern.begin}"
            )
        }

        private fun freeNextFree(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            week: Week, pattern: Hour, nextPattern: Hour,
            lesson: Lesson, nextLesson: Lesson
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
                "$freeLessonStr",
                "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}"
            )
        }

        private fun freeNextAbsence(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            week: Week, pattern: Hour, nextPattern: Hour,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before free lesson

            //during lesson

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$freeLessonStr $untilStr ${pattern.end}",
                "$nextStr: ${nextPattern.begin} ${lesson.change?.typeShortcut} ${lesson.change?.typeName}"
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
                "${getString(R.string.finally_home)}"
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
                "$endStr: ${pattern.end}, $nextStr: ${nextPattern.begin} ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: ${week.rooms.getById(nextLesson.roomId)?.shortcut} - ${week.subjects.getById(nextLesson.subjectId)?.name}",
                "$breakStr ${pattern.end} - ${nextPattern.begin}"
            )
        }

        private fun absenceNextFree(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            week: Week, pattern: Hour, nextPattern: Hour,
            lesson: Lesson, nextLesson: Lesson
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


        private fun getString(id: Int): String {
            return App.getString(id)
        }
    }
}