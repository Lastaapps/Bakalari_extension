package cz.lastaapps.bakalariextension.api.timetable

import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Lesson
import cz.lastaapps.bakalariextension.api.timetable.data.LessonPattern
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.App

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

            val day = week.getDay(TTTools.cal)
            val patterns = week.patterns
            if (day == null)
                return null

            val firstLesson = day.firstLessonIndex()
            val lastLesson = (day.lastLessonIndex() + if (day.endsWithLunch()) 1 else 0)
                .coerceAtMost(day.lessons.size - 1)

            if (firstLesson < 0 || lastLesson < 0)
                return null

            for (index in firstLesson..lastLesson) {

                val pattern = patterns[index]
                val lesson = day.getLesson(index)
                val nextPattern = if (index != lastLesson) patterns[index + 1] else null
                val nextLesson = if (index != lastLesson) day.getLesson(index + 1) else null

                val begin = TTTools.calToSeconds(TTTools.parseTime(pattern.begin, TTTools.CET))
                val end = TTTools.calToSeconds(TTTools.parseTime(pattern.end, TTTools.CET))

                //silent zone before start
                if (index == firstLesson) {
                    actions[begin - (60 * 60)] = null
                }

                // @formatter:off
                //normal lesson
                if (lesson.isNormal()) {

                    if (nextLesson == null || nextPattern == null) {
                        lessonLast(actions, begin, end, pattern, lesson)
                    }
                    else if (nextLesson.isNormal()) {
                        lessonNextLesson(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                    else if (nextLesson.isFree()) {
                        lessonNextFree(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                    else if (nextLesson.isAbsence()) {
                        lessonNextAbsence(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                }
                else if (lesson.isFree()) {
                    //lunch lesson

                    if (nextLesson == null || nextPattern == null) {
                        freeLast(actions, begin, end, pattern, lesson)
                    }
                    else if (nextLesson.isNormal()) {
                        freeNextLesson(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                    else if (nextLesson.isFree()) {
                        freeNextFree(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                    else if (nextLesson.isAbsence()) {
                        freeNextAbsence(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                }
                else if (lesson.isAbsence()) {
                    //if is class absence

                    if (nextLesson == null || nextPattern == null) {
                        absenceLast(actions, begin, end, pattern, lesson)
                    }
                    else if (nextLesson.isNormal()) {
                        absenceNextLesson(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                    else if (nextLesson.isFree()) {
                        absenceNextFree(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                    else if (nextLesson.isAbsence()) {
                        absenceNextAbsence(actions, begin, end, pattern, nextPattern, lesson, nextLesson)
                    }
                }
                // @formatter:on
            }
            return actions
        }

        private fun lessonLast(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern,
            lesson: Lesson
        ) {
            //the last lesson of the day

            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.roomShortcut} - ${lesson.subject}",
                "${lesson.teacher}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${lesson.groupShortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${lesson.roomShortcut} - ${lesson.subject}",
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
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before normal lesson

            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.roomShortcut} - ${lesson.subject}",
                "${lesson.teacher}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${lesson.groupShortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${lesson.roomShortcut} - ${lesson.subject}",
                "$endStr ${pattern.end}, $nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}",
                "$breakStr ${pattern.end} - ${nextPattern.begin}"
            )
        }

        private fun lessonNextFree(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before free lesson

            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.roomShortcut} - ${lesson.subject}",
                "${lesson.teacher}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${lesson.groupShortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${lesson.roomShortcut} - ${lesson.subject}",
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
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before free lesson

            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.roomShortcut} - ${lesson.subject}",
                "${lesson.teacher}${if (lesson.theme != "") " ${lesson.theme}" else ""}, $groupStr ${lesson.groupShortcut}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${lesson.roomShortcut} - ${lesson.subject}",
                "$endStr ${pattern.end}, $nextStr: ${lesson.shortcut} ${lesson.name}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: ${lesson.shortcut} ${lesson.name}",
                "$endStr ${pattern.end}"
            )
        }

        private fun freeLast(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern,
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
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before normal lesson

            //before lesson starts
            //same as during free lesson
            //actions[begin] = arrayOf("", "")

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "$freeLessonStr $untilStr ${pattern.end}",
                "$nextStr: ${nextPattern.begin} ${nextLesson.roomShortcut} - ${nextLesson.subject}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}",
                "$breakStr ${pattern.end} - ${nextPattern.begin}"
            )
        }

        private fun freeNextFree(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern, nextPattern: LessonPattern,
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
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before free lesson

            //during lesson

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$freeLessonStr $untilStr ${pattern.end}",
                "$nextStr: ${nextPattern.begin} ${lesson.shortcut} ${lesson.name}"
            )
        }

        private fun absenceLast(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern,
            lesson: Lesson
        ) {
            //the last lesson - free - probably lunch

            //before lesson starts
            //is same as before lesson end
            //actions[begin] = arrayOf("", "")
            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.shortcut}",
                "${lesson.name}"
            )

            //during lesson
            //is same as before lesson end
            //actions[end - 10 * 60] = arrayOf("", "")

            //last 10 minutes of a lesson
            actions[end] = arrayOf(
                "${lesson.shortcut} ${lesson.name}",
                "${getString(R.string.finally_home)}"
            )
        }

        private fun absenceNextLesson(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before normal lesson

            //before lesson starts
            //same as during free lesson
            //actions[begin] = arrayOf("", "")
            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.shortcut}",
                "${lesson.name}"
            )

            //during lesson
            actions[end - 10 * 60] = arrayOf(
                "${lesson.shortcut} ${lesson.name}",
                "$endStr: ${pattern.end}, $nextStr: ${nextPattern.begin} ${nextLesson.roomShortcut} - ${nextLesson.subject}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "$nextStr: ${nextLesson.roomShortcut} - ${nextLesson.subject}",
                "$breakStr ${pattern.end} - ${nextPattern.begin}"
            )
        }

        private fun absenceNextFree(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before free lesson

            //before lesson starts
            //same whole time
            //actions[begin] = arrayOf("", "")
            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.shortcut}",
                "${lesson.name}"
            )

            //during lesson
            //same whole time
            //actions[end - 10 * 60] = arrayOf("", "")

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "${lesson.shortcut} ${lesson.name}",
                "$nextStr: $freeLessonStr $untilStr ${nextPattern.end}"
            )
        }

        private fun absenceNextAbsence(
            actions: HashMap<Int, Array<String>?>, begin: Int, end: Int,
            pattern: LessonPattern, nextPattern: LessonPattern,
            lesson: Lesson, nextLesson: Lesson
        ) {
            //before free lesson

            //during lesson
            //before lesson starts
            actions[begin] = arrayOf(
                "${pattern.begin} ${lesson.shortcut}",
                "${lesson.name}"
            )

            //last 10 minutes of the lesson
            actions[end] = arrayOf(
                "${lesson.shortcut} ${lesson.name}",
                "$nextStr: ${lesson.shortcut} ${lesson.name}"
            )
        }

        private fun getString(id: Int): String {
            return App.getString(id)
        }
    }
}