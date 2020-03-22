package cz.lastaapps.bakalariextension.tools

import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter

class TimeTools {
    companion object {
        private val TAG = TimeTools::class.java.simpleName

        val COMPLETE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
        val DATE_FORMAT = "yyyy-MM-dd"
        val TIME_FORMAT = "H:mm"
        val UTC = ZoneId.of("UTC")
        val CET = ZoneId.of("Europe/Prague")
        val PERMANENT: ZonedDateTime =
            toMonday(
                toMidnight(
                    ZonedDateTime.of(
                        2001, Month.SEPTEMBER.value, 11,
                        0, 0, 0, 0, UTC
                    )
                )
            )

        fun parse(string: String, pattern: String, timezone: ZoneId? = null): ZonedDateTime {
            var zone = ZonedDateTime.parse(string, DateTimeFormatter.ofPattern(pattern))
            if (timezone != null) {
                zone = zone.toLocalDateTime().atZone(timezone)
            }
            return zone
        }

        fun parseTime(string: String, pattern: String, timezone: ZoneId = UTC): LocalTime {
            var zone = LocalTime.parse(string, DateTimeFormatter.ofPattern(pattern))

            var offset = ZonedDateTime.now(timezone).hour - ZonedDateTime.now(UTC).hour
            if (offset > 0) offset += 24
            zone = zone.minusHours(offset.toLong())

            return zone
        }

        fun format(date: ZonedDateTime, pattern: String, timezone: ZoneId = date.zone): String {
            val newZone = date.withZoneSameInstant(timezone)
            val local = newZone.toLocalDateTime()
            return local.format(DateTimeFormatter.ofPattern(pattern))
        }

        val cal: ZonedDateTime
            get() {
                return toMidnight(
                    ZonedDateTime.now(UTC)
                )
            }

        val now: ZonedDateTime
            get() {
                return ZonedDateTime.now(UTC)
            }

        fun toMidnight(cal: ZonedDateTime): ZonedDateTime {
            val data = cal
                .toLocalDate()
                .atStartOfDay(cal.zone)
            return data
        }

        fun toMonday(cal: ZonedDateTime): ZonedDateTime {
            var diff = DayOfWeek.MONDAY.value - cal.dayOfWeek.value
            if (diff == 0)
                return cal
            while (diff > 0)
                diff -= 7

            val toReturn = cal.plusDays(diff.toLong())
            return toReturn
        }

        fun toDateTime(date: ZonedDateTime, timezone: ZoneId = UTC): LocalDateTime {
            return date.withZoneSameInstant(timezone).toLocalDateTime()
        }

        fun toDate(date: ZonedDateTime, timezone: ZoneId = UTC): LocalDate {
            return date.withZoneSameInstant(timezone).toLocalDate()
        }

        /**@returns seconds till midnight*/
        fun calToSeconds(cal: LocalTime): Int {
            return (cal.hour * 3600
                    + cal.minute * 60
                    + cal.second)
        }

        fun nextWeek(cal: ZonedDateTime): ZonedDateTime {
            return cal.plusDays(7)
        }

        fun previousWeek(cal: ZonedDateTime): ZonedDateTime {
            return cal.minusDays(7)
        }
    }
}