package cz.lastaapps.bakalariextension.api.timetable

import java.text.SimpleDateFormat
import java.util.*

class TTTools {
    companion object {
        private val TAG = TTTools::class.java.simpleName

        const val DATE_FORMAT = "YYYYMMdd"
        const val DAY = 24 * 60 * 60 * 1000
        fun parse(string: String): Calendar {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            val formatter = SimpleDateFormat(DATE_FORMAT)
            formatter.timeZone = TimeZone.getTimeZone("GMT")
            cal.time = formatter.parse(string)!!
            return cal
        }

        fun format(cal: Calendar): String {
            val formatter = SimpleDateFormat(DATE_FORMAT)
            formatter.timeZone = TimeZone.getTimeZone("GMT")
            return formatter.format(cal.time)
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