package cz.lastaapps.bakalariextension.api.timetable.data

import cz.lastaapps.bakalariextension.api.timetable.TTTools
import java.util.*

data class Week(
    var cycleCode: String,
    var cycleName: String,
    var cycleShortcut: String,
    var type: String,
    var patterns: ArrayList<LessonPattern>,
    var days: ArrayList<Day>
) {
    val date: Calendar
        get() {
            if (days[0].date == "")
                return TTTools.PERMANENT

            return TTTools.toMonday(
                TTTools.parse(days[0].date)
            )
        }

    fun getDay(cal: Calendar): Day? {
        return if (days[0].toCal() <= TTTools.toMidnight(cal)
            && TTTools.toMidnight(cal) <= days[4].toCal()
        )

            days[cal.get(Calendar.DAY_OF_WEEK) - 1 - 1]
        else
            null
    }

    fun today(): Day? {
        return getDay(TTTools.cal)
    }


    fun getPatternForLesson(lesson: Lesson): LessonPattern? {
        for (pattern in patterns) {
            if (pattern.caption == lesson.caption)
                return pattern
        }
        return null
    }
}