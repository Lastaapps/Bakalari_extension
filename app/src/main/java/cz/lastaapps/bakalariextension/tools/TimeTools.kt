/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Bakalari extension.
 *
 *     Bakalari extension is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Bakalari extension is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Bakalari extension.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package cz.lastaapps.bakalariextension.tools

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

/**Tool with presets times, parsing and formatting methods and templates*/
class TimeTools {
    companion object {

        const val COMPLETE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX"
        const val COMPLETE_SHORTER = "yyyy-MM-dd'T'HH:mmXXX"
        const val DATE_FORMAT = "yyyy-MM-dd"
        const val TIME_FORMAT = "H:mm"
        val UTC: ZoneId = ZoneId.of("UTC")
        val CET: ZoneId = ZoneId.of("Europe/Prague")

        //used to request permanent timetable
        val PERMANENT: LocalDate =
            LocalDate.of(2001, Month.SEPTEMBER.value, 11).toMonday()


        /**Parses date and time
         * @return parsed ZoneDateTime at selected timezone*/
        fun parse(string: String, pattern: String, timezone: ZoneId? = null): ZonedDateTime {
            var zone = ZonedDateTime.parse(string, DateTimeFormatter.ofPattern(pattern))
            if (timezone != null) {
                zone = zone.toLocalDateTime().atZone(timezone)
            }
            return zone
        }

        /**Parse day only
         * @return parsed Local date*/
        fun parseDate(string: String, pattern: String): LocalDate {
            return LocalDate.parse(string, DateTimeFormatter.ofPattern(pattern))
        }

        /**Parses time only
         * @return parsed LocalTime*/
        fun parseTime(string: String, pattern: String, timezone: ZoneId = UTC): LocalTime {
            var zone = LocalTime.parse(string, DateTimeFormatter.ofPattern(pattern))

            var offset = ZonedDateTime.now(timezone).hour - ZonedDateTime.now(UTC).hour
            if (offset > 0) offset += 24
            zone = zone.minusHours(offset.toLong())

            return zone
        }

        /**@return Formatted date*/
        fun format(date: ZonedDateTime, pattern: String, timezone: ZoneId = date.zone): String {
            val newZone = date.withZoneSameInstant(timezone)
            return newZone.format(DateTimeFormatter.ofPattern(pattern))
        }

        /**Today's midnight in UTC*/
        val today: ZonedDateTime
            get() {
                return ZonedDateTime.now(UTC).toMidnight()
            }

        /**Now in UTC*/
        val now: ZonedDateTime
            get() {
                return ZonedDateTime.now(UTC)
            }

        /**First monday going backward since now in UTC*/
        val monday: ZonedDateTime
            get() {
                return today.toMonday()
            }

        /**Converts to one zone ID common to the whole app for equals method to work properly*/
        fun ZonedDateTime.toCommon(): ZonedDateTime = this.withZoneSameInstant(CET)

        /**Trims dateTimes time to midnight
         * @return previous midnight*/
        fun ZonedDateTime.toMidnight(): ZonedDateTime {
            return this
                .toLocalDate()
                .atStartOfDay(this.zone)
        }

        /**First monday going backward since now in UTC
         * @return monday*/
        fun ZonedDateTime.toMonday(): ZonedDateTime {
            var diff = DayOfWeek.MONDAY.value - this.dayOfWeek.value
            if (diff == 0)
                return this
            while (diff > 0)
                diff -= 7

            return this.plusDays(diff.toLong())
        }

        /**First monday going backward since now in UTC
         * @return monday*/
        fun LocalDate.toMonday(): LocalDate {
            var diff = DayOfWeek.MONDAY.value - this.dayOfWeek.value
            if (diff == 0)
                return this
            while (diff > 0)
                diff -= 7

            return this.plusDays(diff.toLong())
        }

        /**@return LocalDateTime at timezone given*/
        fun ZonedDateTime.toDateTime(timezone: ZoneId = UTC): LocalDateTime =
            this.withZoneSameInstant(timezone).toLocalDateTime()

        /**@return LocalDate at timezone given*/
        fun ZonedDateTime.toDate(timezone: ZoneId = UTC): LocalDate =
            this.withZoneSameInstant(timezone).toLocalDate()

        /**@return LocalTime at timezone given*/
        fun ZonedDateTime.toTime(timezone: ZoneId = UTC): LocalTime =
            this.withZoneSameInstant(timezone).toLocalTime()

        fun ZonedDateTime.toCzechDate(): LocalDate = this.toDate(CET)

        fun LocalDate.toCzechZoned(): ZonedDateTime =
            ZonedDateTime.of(this, LocalTime.MIDNIGHT, CET)

        fun LocalTime.toDaySeconds(): Int = (this.hour * 3600 + this.minute * 60 + this.second)

        /**returns duration in days between these two dates*/
        fun betweenMidnights(date1: ZonedDateTime, date2: ZonedDateTime): Duration {
            return Duration.between(
                LocalDateTime.of(date1.toLocalDate(), LocalTime.MIDNIGHT),
                LocalDateTime.of(date2.toLocalDate(), LocalTime.MIDNIGHT)
            )
        }

        fun ZonedDateTime.toCalendar(): Calendar =
            Calendar.getInstance().also {
                it.time = Date(toInstant().toEpochMilli())
            }

        fun LocalDate.toCalendar(): Calendar =
            ZonedDateTime.of(this, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toCalendar()

        fun Calendar.toZonedDateTime(): ZonedDateTime =
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.time.time), UTC)
    }
}