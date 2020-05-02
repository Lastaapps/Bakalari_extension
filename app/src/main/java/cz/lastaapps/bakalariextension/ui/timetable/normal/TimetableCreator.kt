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

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.timetable.data.Cycle
import cz.lastaapps.bakalariextension.api.timetable.data.Hour
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.timetable.CellSetup
import org.threeten.bp.ZoneId

/**Creates normal timetable cell structure*/
class TimetableCreator {
    companion object {
        fun createTimetable(root: View, week: Week, cycle: Cycle?) {

            val daysTable = root.findViewById<LinearLayout>(R.id.table_days)
            val table = root.findViewById<TableLayout>(R.id.table)

            val context = root.context
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            /**Height of one table row*/
            val height = root.findViewById<ViewGroup>(R.id.table_box).measuredHeight / 6

            //sets up first column
            setupDayNames(
                daysTable,
                week,
                height
            )

            //resets timetable data
            table.removeAllViews()


            //init cycle name, cycle in week is null during empty weeks
            val edge = daysTable.findViewById<ViewGroup>(R.id.edge)
            edge.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            edge.layoutParams = LinearLayout.LayoutParams(edge.measuredWidth, height)

            //non valid hour would result in empty columns (like zero lessons)
            val validHours = week.getNotEmptyHours()
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

            //sets up first row
            setupHours(
                table,
                week,
                validHours,
                inflater,
                height
            )

            //sets up oder rows
            setupLessons(
                table,
                week,
                cycle,
                validHours,
                inflater,
                height
            )

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
            table: TableLayout, week: Week, validHours: DataIdList<Hour>,
            inflater: LayoutInflater, height: Int
        ) {

            val context = table.context

            //first row with lesson start/end times and captions
            val numberRow = TableRow(context)
            table.addView(numberRow, 0)
            for (i in 0 until week.hours.size) {
                val hour = week.hours[i]

                //deleting free columns
                if (!validHours.contains(hour))
                    continue

                val viewGroup =
                    inflater.inflate(R.layout.timetable_number, numberRow, false)

                //sets texts
                viewGroup.findViewById<TextView>(R.id.caption).text = hour.caption
                viewGroup.findViewById<TextView>(R.id.begin).text = hour.begin
                viewGroup.findViewById<TextView>(R.id.end).text = hour.end

                //sets size to match the others
                val params = TableRow.LayoutParams()
                params.height = height
                viewGroup.layoutParams = params
                numberRow.addView(viewGroup)

            }
        }

        /**Sets up lesson view for all the days*/
        private fun setupLessons(
            table: TableLayout, week: Week, cycle: Cycle?,
            validHours: DataIdList<Hour>, inflater: LayoutInflater, height: Int
        ) {
            val context = table.context

            //creating actual timetable
            for (day in week.days) {

                if (!day.isHoliday() || week.isPermanent()) {

                    //regular day
                    val row = TableRow(context)
                    table.addView(row)

                    for (i in 0 until week.hours.size) {
                        val hour = week.hours[i]

                        //deleting free columns
                        if (!validHours.contains(hour))
                            continue

                        val view = inflater.inflate(R.layout.timetable_lesson, row, false)

                        //updates texts in the cell
                        CellSetup.setUpCell(
                            view,
                            week,
                            day,
                            hour,
                            cycle
                        )

                        //shows info on click
                        val lesson = day.getLesson(hour, cycle)
                        if (lesson != null) {
                            view.setOnClickListener(
                                CellSetup.ShowLessonInfo(
                                    week,
                                    day,
                                    hour,
                                    cycle
                                )
                            )
                        }

                        //changes size to match the others
                        val params = TableRow.LayoutParams()
                        params.width = App.getDimension(R.dimen.timetable_column_size)
                        params.height = height
                        view.layoutParams = params
                        view.minimumWidth = params.width

                        row.gravity = Gravity.CENTER_VERTICAL
                        row.addView(view)
                    }
                } else {

                    //during holiday
                    val holiday =
                        inflater.inflate(R.layout.timetable_holiday, table, false)

                    //updates texts
                    holiday.findViewById<TextView>(R.id.holiday).text = day.getHolidayDescription()
                    holiday.findViewById<TextView>(R.id.holiday).textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START

                    //changes size to match the others
                    val params = TableLayout.LayoutParams()
                    params.width = App.getDimension(R.dimen.timetable_column_size) * validHours.size
                    params.height = height
                    holiday.layoutParams = params

                    table.addView(holiday)
                }
            }
        }


    }

}