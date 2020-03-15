package cz.lastaapps.bakalariextension.api.timetable.data

data class LessonPattern (
    var begin: String,
    var end: String,
    var caption: String
): Comparable<LessonPattern> {
    override fun compareTo(other: LessonPattern): Int {
        return caption.toInt().compareTo(other.caption.toInt())
    }
}