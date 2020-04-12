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
import android.view.LayoutInflater
import android.view.View
import android.widget.GridView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.data.Day
import cz.lastaapps.bakalariextension.api.timetable.data.Week

class SmallTimetableView : RelativeLayout {

    companion object {
        private val TAG = SmallTimetableView::class.java.simpleName
    }

    var root: View
    var progressBar: ProgressBar
    var errorMessage: TextView
    var table: GridView
    var holiday: RelativeLayout

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        root = inflater.inflate(R.layout.timetable_small, this, false)
        progressBar = root.findViewById(R.id.progress_bar)
        errorMessage = root.findViewById(R.id.error_message)
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
        progressBar.visibility = View.VISIBLE
        errorMessage.visibility = View.GONE
        table.visibility = View.INVISIBLE
        holiday.visibility = View.GONE
    }

    fun setError(message: String) {
        progressBar.visibility = View.GONE
        errorMessage.visibility = View.VISIBLE
        table.visibility = View.INVISIBLE

        errorMessage.text = message
    }

    fun updateTimetable(week: Week, day: Day) {
        progressBar.visibility = View.GONE
        errorMessage.visibility = View.GONE

        if (!day.isHoliday()) {
            table.visibility = View.VISIBLE
            holiday.visibility = View.GONE

            createTimetable(week, day)
        } else {
            table.visibility = View.GONE
            holiday.visibility = View.VISIBLE

            holiday.findViewById<TextView>(R.id.holiday).text = day.getHolidayDescription()
        }
    }

    private fun createTimetable(week: Week, day: Day) {

        val adapter =
            SmallTimetableAdapter(
                context,
                week,
                day
            )

        if (!adapter.valid()) {
            setError(resources.getString(R.string.empty_timetable))
            return
        }

        table.adapter = adapter
    }

}