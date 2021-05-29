/*
 *    Copyright 2021, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalari.api.entity.absence

import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.tools.TimeTools.toCzechDate
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.Month
import java.time.Month.*
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList

/** Generated in code, holds data for all days with absence data from one month*/
@Parcelize
data class AbsenceMonth(
    val monthName: String,
    val monthIndex: Int,
    override val unsolved: Int,
    override val ok: Int,
    override val missed: Int,
    override val late: Int,
    override val soon: Int,
    override val school: Int
) : AbsenceWrapper(monthIndex.toLong(), unsolved, ok, missed, late, soon, school),
    Comparable<AbsenceMonth> {

    override fun compareTo(other: AbsenceMonth): Int {
        val firstSemester =
            arrayOf(SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER, JANUARY).map { it.value }

        return if (monthIndex in firstSemester && other.monthIndex !in firstSemester) {
            1
        } else if (monthIndex !in firstSemester && other.monthIndex in firstSemester) {
            -1
        } else
            monthIndex.compareTo(other.monthIndex)
    }

    override fun getLabel(): String {
        return monthName
    }

    companion object {

        fun daysToMonths(
            locale: Locale,
            firstSeptember: LocalDate,
            list: List<AbsenceDay>
        ): DataIdList<AbsenceMonth> {
            if (list.isEmpty())
                return DataIdList()

            val someDate = list[0].date
            val firstJanuary = LocalDate.of(firstSeptember.year + 1, FEBRUARY, 1)
            val firstSemester = someDate.toCzechDate() < firstJanuary

            val months = if (firstSemester) {
                arrayOf(SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER, JANUARY)
            } else {
                arrayOf(FEBRUARY, MARCH, APRIL, MAY, JUNE)
            }

            val toReturn = ArrayList<AbsenceMonth>()

            for (month in months) {
                toReturn.add(createMonth(locale, month, list))
            }

            return DataIdList(toReturn.sorted())
        }

        private fun createMonth(
            locale: Locale,
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
                val date = day.date

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
                month.getDisplayName(TextStyle.FULL, locale),
                month.value, unsolved, ok, missed, late, soon, school
            )
        }
    }
}