package cz.lastaapps.bakalariextension.api.timetable

import java.text.SimpleDateFormat
import java.util.*

class TTTools {
    companion object {
        private val TAG = TTTools::class.java.simpleName

        const val DATE_FORMAT = "YYYYMMdd"
        const val TIME_FORMAT = "HH:mm"
        const val CET = "CET"
        const val DAY = 24 * 60 * 60 * 1000
        val PERMANENT: Calendar = toMonday(toMidnight(
            Calendar.getInstance().apply { set(2001, 9 - 1, 11) }
        ))

        fun parse(string: String, timezone: String = "GMT"): Calendar {
            val c = cal
            cal.timeZone = TimeZone.getTimeZone(timezone)
            /*val formatter = SimpleDateFormat(DATE_FORMAT)
            formatter.timeZone = TimeZone.getTimeZone(timezone)
            formatter.isLenient = false;
            c.time = formatter.parse(string)*/

            c.set(Calendar.YEAR, string.substring(0, 4).toInt())
            c.set(Calendar.MONTH, string.substring(4, 6).toInt() - 1)
            c.set(Calendar.DAY_OF_MONTH, string.substring(6, 8).toInt())

            return c
        }

        fun parseTime(string: String, timezone: String = "GMT"): Calendar {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            val formatter = SimpleDateFormat(TIME_FORMAT)
            formatter.timeZone = TimeZone.getTimeZone(timezone)
            cal.time = formatter.parse(string)!!
            return cal
        }

        fun format(cal: Calendar, timezone: String = "GMT", pattern: String = DATE_FORMAT): String {
            val formatter = SimpleDateFormat(pattern)
            formatter.timeZone = TimeZone.getTimeZone(timezone)
            return formatter.format(cal.time)
        }

        fun formatTime(
            cal: Calendar,
            timezone: String = "GMT",
            format: String = TIME_FORMAT
        ): String {
            val formatter = SimpleDateFormat(format)
            formatter.timeZone = TimeZone.getTimeZone(timezone)
            return formatter.format(cal.time)
        }

        val cal: Calendar
            get() {
                return toMidnight(Calendar.getInstance(TimeZone.getTimeZone("GMT")))
            }

        val now: Calendar
        get() {
            return Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        }

        fun toMidnight(cal: Calendar): Calendar {
            cal.set(Calendar.HOUR, 2)//to be sure about timezones
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal
        }

        fun toMonday(cal: Calendar): Calendar {
            var diff = Calendar.MONDAY - cal.get(Calendar.DAY_OF_WEEK)
            if (diff == 0)
                return cal
            while (diff > 0)
                diff -= 7

            var time = cal.time.time
            time += diff * DAY
            cal.time = Date(time)
            return cal
        }

        /**@returns seconds till midnight*/
        fun calToSeconds(cal: Calendar): Int {
            return (cal.get(Calendar.HOUR_OF_DAY) * 3600
                    + cal.get(Calendar.MINUTE) * 60
                    + cal.get(Calendar.SECOND))
        }

        fun nextWeek(cal: Calendar): Calendar {
            cal.time = Date(cal.time.time + 7 * DAY)
            return cal
        }

        fun previousWeek(cal: Calendar): Calendar {
            cal.time = Date(cal.time.time - 7 * DAY)
            return cal
        }
    }
}