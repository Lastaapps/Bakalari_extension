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

package cz.lastaapps.bakalariextension.api.absence.data

import android.content.Context
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.tools.LocaleManager
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.android.parcel.Parcelize
import java.time.Month
import java.time.Month.*
import java.time.format.TextStyle

/** Generated in code, holds data for all days with absence data from one month*/
@Parcelize
class AbsenceMonth(
    val monthName: String,
    val monthIndex: Int,
    override val unsolved: Int,
    override val ok: Int,
    override val missed: Int,
    override val late: Int,
    override val soon: Int,
    override val school: Int
) : AbsenceDataHolder(monthIndex, unsolved, ok, missed, late, soon, school),
    Comparable<AbsenceMonth> {

    override fun compareTo(other: AbsenceMonth): Int {
        return monthIndex.compareTo(other.monthIndex)
    }

    override fun getLabel(): String {
        return monthName
    }

    companion object {

        fun daysToMonths(context: Context, list: List<AbsenceDay>): DataIdList<AbsenceMonth> {
            if (list.isEmpty())
                return DataIdList()

            val someDate = list[0].toDate()
            val firstJanuary =
                TimeTools.firstSeptember
                    .withDayOfMonth(1)
                    .withMonth(FEBRUARY.value)
                    .plusYears(1)
            val firstSemester = someDate < firstJanuary

            val months = if (firstSemester) {
                arrayOf(SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER, JANUARY)
            } else {
                arrayOf(FEBRUARY, MARCH, APRIL, MAY, JUNE)
            }

            val toReturn = ArrayList<AbsenceMonth>()

            for (month in months) {
                toReturn.add(createMonth(context, month, list))
            }

            return DataIdList(toReturn.sorted())
        }

        private fun createMonth(
            context: Context,
            month: Month,
            days: List<AbsenceDay>
        ): AbsenceMonth {
            var unsolved = 0
            var ok = 0
            var missed = 0
            var late = 0
            var soon = 0
            var school = 0

            for (day in days) {
                val date = day.toDate()

                if (date.month == month) {
                    unsolved += day.unsolved
                    ok += day.ok
                    missed += day.missed
                    late += day.late
                    soon += day.soon
                    school += day.school
                }
            }

            return AbsenceMonth(
                month.getDisplayName(
                    TextStyle.FULL,
                    LocaleManager.getLocale(context)
                ), month.value, unsolved, ok, missed, late, soon, school
            )
        }
    }
}