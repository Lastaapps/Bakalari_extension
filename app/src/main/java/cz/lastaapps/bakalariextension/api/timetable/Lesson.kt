package cz.lastaapps.bakalariextension.api.timetable

data class Lesson(
    var id: String,
    var type: String,
    var subject: String,
    var subjectShortcut: String,
    var teacher: String,
    var teacherShortcut: String,
    var room: String,
    var roomShortcut: String,
    var absence: String,
    var absenceShortcut: String,
    var theme: String,
    var group: String,
    var groupShortcut: String,
    var cycle: String,
    var freed: String,
    var change: String,
    var caption: String,
    var notice: String
): Comparable<Lesson> {

    fun isFree(): Boolean {
        return type == "X"
    }

    override fun compareTo(other: Lesson): Int {
        return caption.toInt().compareTo(other.caption.toInt())
    }
}