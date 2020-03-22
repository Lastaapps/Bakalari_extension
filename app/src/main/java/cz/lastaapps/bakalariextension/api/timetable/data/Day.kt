package cz.lastaapps.bakalariextension.api.timetable.data

import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZonedDateTime

data class Day (
    var dayOfWeek: Int,
    var date: String,
    var description: String,
    var dayType: String,
    var lessons: DataIdList<Lesson>
): Comparable<Day> {
    override fun compareTo(other: Day): Int {
        return TimeTools.parse(
            date, TimeTools.COMPLETE_FORMAT
        ).compareTo(
            TimeTools.parse(
                other.date, TimeTools.COMPLETE_FORMAT
            )
        )
    }

    fun getLesson(hour: Hour, cycle: TTData.Cycle? = null): Lesson? {
        if (cycle == null)
            return lessons.getById(hour.id)

        val array = lessons.getAllById(hour.id)
        for (lesson in array) {
            if (lesson.cycleIds.contains(cycle.id))
                return lesson
        }
        return null
    }

    fun isFree(hour: Hour): Boolean {
        val lesson = getLesson(hour) ?: return true
        if (lesson.isRemoved()) return true
        return false
    }

    fun isNormal(hour: Hour): Boolean {
        val lesson = getLesson(hour) ?: return false
        return lesson.isNormal()
    }

    fun isAbsence(hour: Hour): Boolean {
        val lesson = getLesson(hour) ?: return false
        return lesson.isAbsence()
    }

    fun firstLessonIndex(hours: DataIdList<Hour>): Int {
        for (i in 0 until hours.size) {
            if (!isFree(hours[i]))
                return i
        }
        return -1
    }

    fun lastLessonIndex(hours: DataIdList<Hour>): Int {
        for (i in (hours.size - 1) downTo 0) {
            if (!isFree(hours[i]))
                return i
        }
        return -1
    }

    fun endsWithLunch(hours: DataIdList<Hour>): Boolean {
        for (i in firstLessonIndex(hours)..lastLessonIndex(hours)) {
            if (i > 0) return false

            if (isFree(hours[i])) {
                return false
            }
        }
        return true
    }

    fun isEmpty(): Boolean {
        for (lesson in lessons) {
            if (!lesson.isRemoved()) {
                return false
            }
        }
        return true
    }

    fun toDate(): ZonedDateTime {
        if (date == "")
            return TimeTools.PERMANENT

       return TimeTools.parse(date, TimeTools.COMPLETE_FORMAT)
    }
}
