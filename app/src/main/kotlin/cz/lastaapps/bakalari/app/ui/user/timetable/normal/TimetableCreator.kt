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

package cz.lastaapps.bakalari.app.ui.user.timetable.normal

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.api.entity.homework.HomeworkList
import cz.lastaapps.bakalari.api.entity.timetable.Hour
import cz.lastaapps.bakalari.api.entity.timetable.Week
import cz.lastaapps.bakalari.api.entity.user.User
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.user.timetable.CellSetup
import cz.lastaapps.bakalari.platform.App
import kotlinx.coroutines.yield
import java.time.format.DateTimeFormatter

/**Creates normal timetable cell structure*/
class TimetableCreator {
    companion object {
        private val TAG get() = TimetableCreator::class.java.simpleName

        fun prepareTimetable(root: View, totalHeight: Int, days: Int, lessons: Int) {
            Log.i(TAG, "Preparing timetable views")

            val height = totalHeight / (1 + days)

            val table = root.findViewById<TableLayout>(R.id.table)

            val rowChildParams = TableRow.LayoutParams()
            rowChildParams.height = height
            rowChildParams.width = App.getDimension(R.dimen.timetable_column_size)

            val tableChildParams = TableLayout.LayoutParams()
            tableChildParams.height = height
            tableChildParams.width = App.getDimension(R.dimen.timetable_column_size) * lessons

            val inflater = LayoutInflater.from(root.context)

            for (i in 0 until (1 + days * 2)) {

                //view is TableRow of Holiday
                val view = table.getChildAt(i) as ViewGroup
                view.layoutParams = tableChildParams

                when {
                    i == 0 -> {
                        //lesson number

                        //inflates new views if needed
                        while (view.childCount < lessons) {
                            inflater.inflate(R.layout.timetable_hour, view, true)
                        }

                        //hides surplus cells
                        for (j in 0 until view.childCount) {
                            view.getChildAt(j).apply {
                                visibility =
                                    if (j <= lessons) {
                                        View.VISIBLE
                                    } else {
                                        View.GONE
                                    }
                                layoutParams = rowChildParams
                            }
                        }
                    }
                    i % 2 == 1 -> {
                        //normal lessons

                        //inflates new views if needed
                        while (view.childCount < lessons) {
                            inflater.inflate(R.layout.timetable_lesson, view, true)
                        }

                        //hides surplus cells
                        for (j in 0 until view.childCount) {
                            view.getChildAt(j).apply {
                                visibility =
                                    if (j <= lessons) {
                                        View.VISIBLE
                                    } else {
                                        View.GONE
                                    }
                                layoutParams = rowChildParams
                            }
                        }
                    }
                    else -> {
                        //holiday

                        //updates background of holiday, default w and h is match_parent
                        view.getChildAt(0).layoutParams =
                            RelativeLayout.LayoutParams(tableChildParams)
                        view.findViewById<TextView>(R.id.holiday).textAlignment =
                            TextView.TEXT_ALIGNMENT_VIEW_START
                    }
                }
            }

            val daysTable = root.findViewById<LinearLayout>(R.id.table_days)

            val dayParams = LinearLayout.LayoutParams(
                App.getDimension(R.dimen.timetable_column_size_first),
                height
            )

            for (i in 0 until 1 + days) {//edge and 5 days
                val view = daysTable.getChildAt(i)
                view.layoutParams = dayParams
            }
        }

        suspend fun createTimetable(
            root: View,
            week: Week,
            cycle: SimpleData?,
            user: User,
            homework: HomeworkList?
        ) {

            Log.i(TAG, "Creating timetable")

            val daysTable = root.findViewById<LinearLayout>(R.id.table_days)
            val table = root.findViewById<TableLayout>(R.id.table)

            //sets up first column
            setupDayNames(
                daysTable,
                week
            )

            yield()

            //non valid hour would result in empty columns (like zero lessons)
            val validHours = week.trimFreeMorning()

            //sets up first row
            setupHours(
                table,
                week,
                validHours
            )

            yield()

            //sets up oder rows
            setupLessons(
                table,
                week,
                cycle,
                validHours,
                user,
                homework
            )

            Log.i(TAG, "Creating finished")
        }

        /**Sets up first column Mo-Fr except first edge cell*/
        private fun setupDayNames(daysTable: LinearLayout, week: Week) {

            //changes day's days and shortcuts in first day column
            val dayArray =
                arrayOf(R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday)
            val dayShortcutsArray = arrayOf(
                R.string.monday_shortcut,
                R.string.tuesday_shortcut,
                R.string.wednesday_shortcut,
                R.string.thursday_shortcut,
                R.string.friday_shortcut
            )

            val mondayDate = week.monday

            //goes through
            var viewIndex = 1//edge skipping
            for (i in 0 until 5) {

                if (week.getDayOfWeek(i + 1) == null) continue

                val view = daysTable.getChildAt(viewIndex++)

                val dayTV = view.findViewById<TextView>(R.id.day)
                val dateTV = view.findViewById<TextView>(R.id.date)

                dayTV.text = App.getString(dayShortcutsArray[i])
                //permanent timetable doesn't show date
                if (!week.isPermanent())
                    dateTV.text =
                        mondayDate.plusDays(i.toLong()).format(DateTimeFormatter.ofPattern("d.M."))
                else
                //For permanent timetable
                    dateTV.text = ""
            }
        }

        /**Sets up first row except first edge cell*/
        private fun setupHours(
            table: TableLayout, week: Week, validHours: DataIdList<Hour>
        ) {

            //first row with lesson start/end times and captions
            val numberRow = table.getChildAt(0) as TableRow

            for (i in 0 until validHours.size) {
                val hour = validHours[i]

                val viewGroup = numberRow.getChildAt(i)

                //sets texts
                viewGroup.findViewById<TextView>(R.id.caption).text = hour.caption
                viewGroup.findViewById<TextView>(R.id.begin).text = hour.begin
                viewGroup.findViewById<TextView>(R.id.end).text = hour.end
            }
        }

        /**Sets up lesson view for all the days*/
        private suspend fun setupLessons(
            table: TableLayout,
            week: Week,
            cycle: SimpleData?,
            validHours: DataIdList<Hour>,
            user: User,
            homework: HomeworkList?
        ) {
            //creating actual timetable
            for (i in 0 until week.days.size) {
                val day = week.days[i]

                //regular day
                val row = table.getChildAt(i * 2 + 1) as TableRow
                val holiday = table.getChildAt(i * 2 + 2)

                if (!day.isHoliday() || week.isPermanent()) {

                    for (j in 0 until validHours.size) {
                        val hour = validHours[j]

                        val cell = row.getChildAt(j)

                        //updates texts in the cell
                        CellSetup.setUpCell(
                            cell,
                            week,
                            day,
                            hour,
                            cycle,
                            homework
                        )

                        //shows info on click
                        val lesson = day.getLesson(hour, cycle)
                        if (lesson != null) {
                            cell.setOnClickListener(
                                CellSetup.ShowLessonInfo(
                                    week,
                                    day,
                                    hour,
                                    cycle,
                                    user,
                                    homework
                                )
                            )
                        }

                        row.visibility = View.VISIBLE
                        holiday.visibility = View.GONE
                    }
                } else {

                    //during holiday

                    //updates texts
                    holiday.findViewById<TextView>(R.id.holiday).text =
                        day.getHolidayDescription(table.context, true)

                    row.visibility = View.GONE
                    holiday.visibility = View.VISIBLE
                }
            }

            yield()
        }
    }
}