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
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Cycle
import cz.lastaapps.bakalariextension.api.timetable.data.Day
import cz.lastaapps.bakalariextension.api.timetable.data.Hour
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.ui.timetable.CellSetup

class TableAdapter(
    val context: Context,
    private var week: Week,
    private var cycle: Cycle?,
    width: Int, height: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val typeHour = 0
        const val typeLesson = 1
        const val typeHoliday = 2
    }

    var validHours = week.trimFreeMorning()

    private val inflater = LayoutInflater.from(context)
    private val params = RelativeLayout.LayoutParams(width, height)
    private val holidayParams = RelativeLayout.LayoutParams(width * validHours.size, height)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(
            when (viewType) {
                typeHour -> {
                    inflater.inflate(R.layout.timetable_hour, parent, false)
                        .apply { layoutParams = params }
                }
                typeLesson -> {
                    inflater.inflate(R.layout.timetable_lesson, parent, false).apply {
                        layoutParams = params
                    }
                }
                else -> {
                    (inflater.inflate(
                        R.layout.timetable_holiday,
                        parent,
                        false
                    ) as ViewGroup).apply {
                        layoutParams = holidayParams
                        getChildAt(0).layoutParams = holidayParams
                    }
                }
            }) {}
    }

    override fun getItemCount(): Int {
        var length = validHours.size
        for (day in week.days) {
            length += dayLength(day)
        }
        return length
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val cell = holder.itemView
        when (holder.itemViewType) {
            typeHour -> {
                val hour = getHour(position)
                cell.findViewById<TextView>(R.id.caption).text = hour.caption
                cell.findViewById<TextView>(R.id.begin).text = hour.begin
                cell.findViewById<TextView>(R.id.end).text = hour.end
            }
            typeLesson -> {
                val day = getDay(position)!!
                val hour = getHour(position)

                CellSetup.setUpCell(
                    cell,
                    week,
                    day,
                    hour,
                    cycle
                )

                //todo
                //shows info on click
                val lesson = day.getLesson(hour, cycle)
                if (lesson != null) {
                    cell.setOnClickListener(
                        CellSetup.ShowLessonInfo(
                            week,
                            day,
                            hour,
                            cycle
                        )
                    )
                }
            }
            else -> {

                //holiday
                //updates texts
                val day = getDay(position)!!

                cell.findViewById<TextView>(R.id.holiday).text = day.getHolidayDescription()
                cell.findViewById<TextView>(R.id.holiday).textAlignment =
                    TextView.TEXT_ALIGNMENT_VIEW_START
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position < validHours.size)
            return typeHour

        val day = getDay(position)!!
        return if (!day.isHoliday()) {
            typeLesson
        } else {
            typeHoliday
        }
    }

    fun updateWeek(week: Week, cycle: Cycle?) {
        this.week = week
        this.cycle

        validHours = week.trimFreeMorning()
        holidayParams.width = params.width * validHours.size

        //Triggers the list update
        notifyDataSetChanged()
    }

    private fun dayLength(day: Day): Int {
        return if (!day.isHoliday())
            validHours.size
        else
            1
    }

    private fun getDay(position: Int): Day? {
        var vh = validHours.size
        if (position < vh)
            return null

        for (day in week.days) {
            if (position in vh until vh + dayLength(day))
                return day
            vh += dayLength(day)
        }
        return null
    }

    private fun getHour(position: Int): Hour {
        var vh = validHours.size
        if (position < vh)
            return validHours[position]

        for (day in week.days) {
            if (position in vh until vh + dayLength(day))
                return validHours[position - vh]
            vh += dayLength(day)
        }
        return validHours[0]
    }
}