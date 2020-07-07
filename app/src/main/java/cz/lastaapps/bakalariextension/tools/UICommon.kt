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

import android.content.Context
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import java.text.Normalizer
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

//contains methods used through whole UI section

/**@return String representing when was something last updated in human readable form
 * Last updated 2 days ago*/
fun lastUpdated(context: Context, lastUpdated: ZonedDateTime): String {
    return formatTimeDifference(context, lastUpdated, TimeTools.now)
}

fun formatTimeDifference(context: Context, start: ZonedDateTime, end: ZonedDateTime): String {
    val dur = Duration.between(start, end)!!

    //String in which are data formatted them, has %d and %s
    val lastUpdatedResource = context.getString(R.string.last_updated_template)

    return when {
        //if someone is changing device time
        dur.isNegative -> {
            context.getString(R.string.last_updated_in_future)
        }

        //last 59 sec
        dur.seconds < 60 -> {
            context.getString(R.string.last_updated_right_now)
        }

        //last 59 minutes
        dur.toMinutes() < 60 -> {
            //check if minute or minutes should be used
            val index = if (dur.toMinutes() == 1L) 0 else 1

            String.format(
                lastUpdatedResource,
                dur.toMinutes(),
                context.resources.getQuantityString(
                    R.plurals.last_updated_minutes,
                    dur.toMinutes().toInt()
                )
            )
        }

        //last 23 hours
        dur.toHours() < 24 -> {
            val index = if (dur.toHours() == 1L) 0 else 1

            String.format(
                lastUpdatedResource,
                dur.toHours(),
                context.resources.getQuantityString(
                    R.plurals.last_updated_hours,
                    dur.toMinutes().toInt()
                )
            )
        }

        //last 6 days
        dur.toDays() < 7 -> {
            val index = if (dur.toDays() == 1L) 0 else 1

            String.format(
                lastUpdatedResource,
                dur.toDays(),
                context.resources.getQuantityString(
                    R.plurals.last_updated_days,
                    dur.toMinutes().toInt()
                )
            )
        }

        //last x weeks
        else -> {
            val index = if (dur.toDays() / 7 == 1L) 0 else 1

            String.format(
                lastUpdatedResource,
                dur.toDays() / 7,
                context.resources.getQuantityString(
                    R.plurals.last_updated_weeks,
                    dur.toMinutes().toInt()
                )
            )
        }
    }
}

/** Replaces diacritics from text with their basic alternative and makes the text lowercase*/
fun searchNeutralText(input: String): String {
    val lower = input.toLowerCase(Locale.ROOT)
    return Normalizer.normalize(lower, Normalizer.Form.NFD)
        .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
}

fun validDate(week: Week?, settKey: String, arrayId: Int): ZonedDateTime? {
    val toReturn: ZonedDateTime

    val sett = MySettings.withAppContext()
    val weekRequired = sett.weekRequired(settKey, arrayId)

    if (weekRequired) {

        //week object required to determinate these data
        week ?: return null

        val hours = week.hours
        val today = week.today()

        //in the evening tomorrows timetable can be loaded
        val now = TimeTools.now
        toReturn = MySettings.withAppContext().showTomorrow(
            now,

            if (today != null) {

                val index = today.lastLessonIndex(hours, null)
                if (index >= 0) {

                    //for normal days, gets end of the last lesson
                    val hour = hours[index]
                    val endTime =
                        TimeTools.parseTime(hour.end, TimeTools.TIME_FORMAT, TimeTools.CET)

                    ZonedDateTime.of(
                        TimeTools.today.toLocalDate(),
                        endTime,
                        TimeTools.CET
                    )

                } else {

                    //for holidays and empty days
                    TimeTools.today.withHour(14)
                }
            } else {
                //for weekend, returns next Monday
                now
            },
            settKey, arrayId
        )
    } else {

        //values like midnight or 5 p.m. works any time, for example now
        //bots now have to be the same or to work properly
        val now = TimeTools.now
        toReturn = MySettings.withAppContext().showTomorrow(
            now, now,
            settKey, arrayId
        )
    }

    return toReturn
}


