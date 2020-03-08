package cz.lastaapps.bakalariextension.api.timetable

data class Day (
    var date: String,
    var dayShortcut: String,
    var lessons: ArrayList<Lesson>
): Comparable<Day> {
    override fun compareTo(other: Day): Int {
        return TTTools.parse(date).compareTo(
            TTTools.parse(other.date))
    }

    fun lastLessonIndex(): Int {
        for (i in (lessons.size - 1)..0) {
            val lesson = lessons[i]
            if (!lesson.isFree()) {
                return (i + 1).coerceAtMost(lessons.size - 1)
            }
        }
        return 0
    }
}
