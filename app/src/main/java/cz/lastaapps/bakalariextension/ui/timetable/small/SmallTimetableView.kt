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

package cz.lastaapps.bakalariextension.ui.timetable.small

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.GridView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.api.timetable.data.Day
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.api.user.data.User
import cz.lastaapps.bakalariextension.tools.TimeTools
import java.time.ZonedDateTime

class SmallTimetableView : RelativeLayout {

    companion object {
        private val TAG = SmallTimetableView::class.java.simpleName
    }

    val root: View
    val progressBar: ProgressBar
    val errorMessage: TextView
    val date: TextView
    val table: GridView
    val holiday: RelativeLayout

    init {
        Log.i(TAG, "Init")

        root = LayoutInflater.from(context).inflate(R.layout.fragment_timetable_small, this, false)

        progressBar = root.findViewById(R.id.progress_bar)
        errorMessage = root.findViewById(R.id.error_message)
        date = root.findViewById(R.id.date_label)
        table = root.findViewById(R.id.table)
        holiday = root.findViewById(R.id.cell_main)

        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        root.layoutParams = params
        addView(root)

        val holidayParams = ConstraintLayout.LayoutParams(holiday.layoutParams)
        holidayParams.height = App.getDimension(R.dimen.timetable_row_height)
        holiday.layoutParams = holidayParams

        setLoading()
    }

    constructor(context: Context): super(context)

    constructor(context: Context, set: AttributeSet): super(context, set)

    fun setLoading() {
        Log.i(TAG, "Loading")

        progressBar.visibility = View.VISIBLE
        errorMessage.visibility = View.GONE
        table.visibility = View.INVISIBLE
        holiday.visibility = View.GONE
    }

    fun setError(message: String) {
        Log.i(TAG, "Error: $message")

        progressBar.visibility = View.GONE
        errorMessage.visibility = View.VISIBLE
        table.visibility = View.INVISIBLE

        errorMessage.text = message
    }

    fun setDate(dateTime: ZonedDateTime) {
        date.text = TimeTools.format(dateTime, "E, d. MMMM")
    }

    fun updateTimetable(week: Week, day: Day, user: User, homework: HomeworkList?) {
        progressBar.visibility = View.GONE
        errorMessage.visibility = View.GONE

        if (!day.isHoliday()) {
            table.visibility = View.VISIBLE
            holiday.visibility = View.GONE

            Log.i(TAG, "Creating timetable")
            createTimetable(week, day, user, homework)
        } else {
            Log.i(TAG, "Showing holidays")

            table.visibility = View.GONE
            holiday.visibility = View.VISIBLE

            holiday.findViewById<TextView>(R.id.holiday).text = day.getHolidayDescription()
        }
    }

    private fun createTimetable(week: Week, day: Day, user: User, homework: HomeworkList?) {

        val adapter = SmallTimetableAdapter(
            context,
            week,
            day,
            user,
            homework
        )

        if (!adapter.valid()) {
            setError(resources.getString(R.string.empty_timetable))
            return
        }

        table.adapter = adapter
    }

}