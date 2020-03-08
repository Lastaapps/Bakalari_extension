package cz.lastaapps.bakalariextension.api.timetable

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
    get() {return TTTools.toMonday(TTTools.parse(days[0].date))}
}