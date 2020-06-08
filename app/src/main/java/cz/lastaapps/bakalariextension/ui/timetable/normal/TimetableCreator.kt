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

package cz.lastaapps.bakalariextension.ui.timetable.normal

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.api.timetable.data.Cycle
import cz.lastaapps.bakalariextension.api.timetable.data.Hour
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.timetable.CellSetup
import kotlinx.coroutines.yield
import java.time.ZoneId

/**Creates normal timetable cell structure*/
class TimetableCreator {
    companion object {
        private val TAG = TimetableCreator::class.java.simpleName

        fun prepareTimetable(root: View, height: Int, lessons: Int) {
            Log.i(TAG, "Preparing timetable views")

            val table = root.findViewById<TableLayout>(R.id.table)

            val rowChildParams = TableRow.LayoutParams()
            rowChildParams.height = height
            rowChildParams.width = App.getDimension(R.dimen.timetable_column_size)

            val tableChildParams = TableLayout.LayoutParams()
            tableChildParams.height = height
            tableChildParams.width = App.getDimension(R.dimen.timetable_column_size) * lessons

            val inflater = LayoutInflater.from(root.context)

            for (i in 0 until table.childCount) {

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
                    }
                }
            }
        }

        suspend fun createTimetable(
            root: View,
            week: Week,
            cycle: Cycle?,
            homework: HomeworkList?
        ) {

            Log.i(TAG, "Creating timetable")

            val daysTable = root.findViewById<LinearLayout>(R.id.table_days)
            val table = root.findViewById<TableLayout>(R.id.table)

            /**Height of one table row*/
            val height = root.findViewById<ViewGroup>(R.id.table_box).measuredHeight / 6

            //sets up first column
            setupDayNames(
                daysTable,
                week,
                height
            )

            yield()

            //init cycle name, cycle in week is null during empty weeks
            val edge = daysTable.findViewById<ViewGroup>(R.id.edge)
            edge.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            edge.layoutParams = LinearLayout.LayoutParams(edge.measuredWidth, height)

            //non valid hour would result in empty columns (like zero lessons)
            val validHours = week.trimFreeMorning()
            if (validHours.isEmpty() || cycle == null) {

                edge.findViewById<TextView>(R.id.cycle).text = ""

                root.findViewById<TextView>(R.id.empty_timetable)
                    .visibility = View.VISIBLE

                return
            } else {

                //init cycle name
                edge.findViewById<TextView>(R.id.cycle).text =
                    cycle.name

                root.findViewById<TextView>(R.id.empty_timetable)
                    .visibility = View.GONE
            }

            yield()

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
                homework
            )

            Log.i(TAG, "Creating finished")
        }

        /**Sets up first column Mo-Fr except first edge cell*/
        private fun setupDayNames(daysTable: LinearLayout, week: Week, height: Int) {

            //changes day's days and shortcuts in first day column
            val dayArray =
                arrayOf(R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday)
            val dayShortcutsArray = arrayOf(
                R.string.monday_shortut,
                R.string.tuesday_shortut,
                R.string.wednesday_shortut,
                R.string.thursday_shortut,
                R.string.friday_shortut
            )

            //goes through
            for (i in 0 until 5) {
                val day = week.days[i]
                val view = daysTable.findViewById<ViewGroup>(dayArray[i])

                //sets tows height to mach the oder ones
                view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                view.layoutParams = LinearLayout.LayoutParams(view.measuredWidth, height)

                val dayTV = view.findViewById<TextView>(R.id.day)
                val dateTV = view.findViewById<TextView>(R.id.date)

                dayTV.text = App.getString(dayShortcutsArray[i])
                //permanent timetable doesn't show date
                if (!week.isPermanent())
                    dateTV.text = TimeTools.format(day.toDate(), "d.M.", ZoneId.systemDefault())
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
            cycle: Cycle?,
            validHours: DataIdList<Hour>,
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
                    holiday.findViewById<TextView>(R.id.holiday).text = day.getHolidayDescription()
                    holiday.findViewById<TextView>(R.id.holiday).textAlignment =
                        TextView.TEXT_ALIGNMENT_VIEW_START

                    row.visibility = View.GONE
                    holiday.visibility = View.VISIBLE
                }

                yield()
            }
        }


    }

}