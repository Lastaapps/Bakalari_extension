package cz.lastaapps.bakalariextension.api.timetable.data

import cz.lastaapps.bakalariextension.api.timetable.TTTools
import java.util.*

data class Day (
    var date: String,
    var dayShortcut: String,
    var lessons: ArrayList<Lesson>
): Comparable<Day> {
    override fun compareTo(other: Day): Int {
        return TTTools.parse(
            date
        ).compareTo(
            TTTools.parse(
                other.date
            )
        )
    }

    fun firstLessonIndex(): Int {
        for (i in 0 until lessons.size) {
            val lesson = lessons[i]
            if (lesson.isNormal() || lesson.isAbsence()) {
                return i
            }
        }
        return -1
    }

    fun lastLessonIndex(): Int {
        for (i in (lessons.size - 1) downTo 0) {
            val lesson = lessons[i]
            if (lesson.isNormal()) {
                return i
            }
        }
        return -1
    }

    fun endsWithLunch(): Boolean {
        for (index in firstLessonIndex()..lastLessonIndex()) {
            val lesson = getLesson(index)
            if (lesson.isFree())
                return false
        }
        return true
    }

    fun getLesson(index: Int): Lesson {
        return lessons[index]
    }

    fun toCal(): Calendar {
        if (date == "")
            return TTTools.PERMANENT

       return TTTools.toMidnight(TTTools.parse(date))
    }

    fun isEmpty(): Boolean {
        lessons.forEach {
            if (!it.isFree())
                return false
        }
        return true
    }
}
