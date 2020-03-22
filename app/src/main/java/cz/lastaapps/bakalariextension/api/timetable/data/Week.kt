package cz.lastaapps.bakalariextension.api.timetable.data

import cz.lastaapps.bakalariextension.api.timetable.data.TTData.*
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.threeten.bp.ZonedDateTime
import kotlin.math.min

data class Week(
    var hours: DataIdList<Hour>,
    var days: ArrayList<Day>,
    var classes: DataIdList<Class>,
    var groups: DataIdList<Group>,
    var subjects: DataIdList<Subject>,
    var teachers: DataIdList<Teacher>,
    var rooms: DataIdList<Room>,
    var cycles: DataIdList<Cycle>
) {
    val date: ZonedDateTime
        get() {
            val time = days[0].toDate()
            return TimeTools.toMonday(time)
        }

    fun getDay(cal: ZonedDateTime): Day? {

        return if (TimeTools.toDateTime(days[0].toDate()) <= TimeTools.toDateTime(cal)
            && TimeTools.toDateTime(cal) < TimeTools.toDateTime(days[4].toDate()).plusDays(1)
        )
            days[cal.dayOfWeek.value - 1]
        else
            null
    }

    fun today(): Day? {
        return getDay(TimeTools.now)
    }

    fun getNotEmptyHours(): DataIdList<Hour> {
        var min = hours.size
        for (day in days) {
            min = min(day.firstLessonIndex(hours), min)
        }

        val data = DataIdList<Hour>()
        if (min in 0 until hours.size)
            data.addAll(hours.subList(min, hours.size))

        return data
    }
}