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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.homework.data.HomeworkList
import cz.lastaapps.bakalariextension.api.timetable.data.Day
import cz.lastaapps.bakalariextension.api.timetable.data.Lesson
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.ui.timetable.CellSetup
import kotlin.math.max

/**Adapter supplying view for small timetable fragment*/
class SmallTimetableAdapter(
    var context: Context,
    var week: Week,
    var day: Day,
    var homework: HomeworkList?
) : BaseAdapter() {

    //witch lesson should be shown
    private var firstIndex = 0
    private var lastIndex = 0
    private val inflater = LayoutInflater.from(context)

    init {
        firstIndex = day.firstLessonIndex(week.hours)
        lastIndex = day.lastLessonIndex(week.hours)
    }

    /**id day is not empty*/
    fun valid(): Boolean {
        return !(firstIndex < 0 || lastIndex < 0 || lastIndex < firstIndex)
    }

    override fun getView(position: Int, changedView: View?, parent: ViewGroup?): View {
        val view = changedView ?: {

            val view = inflater.inflate(R.layout.timetable_lesson, parent, false)

            //view setup
            view.minimumWidth = App.getDimension(R.dimen.timetable_column_size)
            view.minimumHeight = App.getDimension(R.dimen.timetable_row_height)
            val params = AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, view.layoutParams.height
            )
            view.layoutParams = params

            view
        }.invoke()

        //lesson to be shows
        val hour = week.hours[position + firstIndex]

        //sets texts
        CellSetup.setUpCell(
            view,
            week,
            day,
            hour,
            null,
            homework
        )

        //shows some lesson info on click
        view.setOnClickListener(
            CellSetup.ShowLessonInfo(
                week,
                day,
                hour,
                null,
                homework
            )
        )

        /*view.findViewById<ViewGroup>(R.id.cell_border).apply {
            //setBackgroundColor(App.getColor(android.R.color.transparent))
            Log.e(TAG, "$layoutParams")
            layoutParams = params
        }*/

        return view
    }

    override fun getItem(position: Int): Any? {
        return day.getLesson(week.hours[position + firstIndex])
    }

    override fun getItemId(position: Int): Long {
        val lesson = getItem(position) ?: return 0
        return (lesson as Lesson).id.toLong()
    }

    override fun getCount(): Int {
        return max(0, lastIndex + 1 - firstIndex)
    }
}
