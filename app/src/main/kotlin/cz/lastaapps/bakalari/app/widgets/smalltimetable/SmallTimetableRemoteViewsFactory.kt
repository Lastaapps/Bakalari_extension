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

package cz.lastaapps.bakalari.app.widgets.smalltimetable

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import cz.lastaapps.bakalari.api.entity.timetable.Day
import cz.lastaapps.bakalari.api.entity.timetable.Week
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.start.loading.LoadingFragment
import cz.lastaapps.bakalari.app.ui.user.timetable.CellSetup
import cz.lastaapps.bakalari.platform.App
import kotlin.math.max
import kotlin.math.min

/**Creates views for SmallTimetable widget*/
class SmallTimetableRemoteViewsFactory(
    val context: Context,
    val widgetId: Int,
    val week: Week,
    val day: Day,
) : RemoteViewsService.RemoteViewsFactory {

    companion object {
        private val TAG = RemoteViewsService::class.java.simpleName
    }

    //indexes of edge lessons
    private var firstIndex = 0
    private var lastIndex = 0

    //if day is empty
    private var isEmpty = false

    override fun onCreate() {}

    /**Called after #onCreate and before #getCount*/
    override fun onDataSetChanged() {

        isEmpty = false

        //indexes init
        firstIndex = day.firstLessonIndex(week.hours)
        lastIndex = day.lastLessonIndex(week.hours)

        //if one of them is -1 = no lessons for the day
        if (min(firstIndex, lastIndex) < 0) {
            isEmpty = true
        }
    }

    /**@return Timetable cell*/
    override fun getViewAt(position: Int): RemoteViews {
        //inflated views
        val views = RemoteViews(context.packageName, R.layout.widget_smalltimetable_lesson)

        val hour = week.hours[firstIndex + position]

        //map of strings to put into cell
        val map =
            CellSetup.getStrings(
                week,
                day,
                hour,
                null
            )
        //background color of the cell
        val color =
            CellSetup.getBackgroundColor(
                week,
                day,
                hour,
                null,
                false
            )

        //sets texts
        views.setTextViewText(R.id.subject, map[R.id.subject])
        views.setTextViewText(R.id.teacher, map[R.id.teacher])
        views.setTextViewText(R.id.room, map[R.id.room])

        //sets background
        views.setInt(R.id.cell_main, "setBackgroundColor", color)

        //sets text color based on widget theme
        val array = arrayOf(R.id.subject, R.id.teacher, R.id.room)
        for (it in array) {
            views.setTextColor(
                it, App.getColor(
                    if (SmallTimetableWidgetConfig.updater.isLight(widgetId))
                        R.color.widget_foreground
                    else
                        R.color.widget_foreground_dark
                )
            )
        }

        //opens full timetable
        val intent = Intent(context, LoadingFragment::class.java)
        intent.putExtra(MainActivity.NAVIGATE, R.id.nav_timetable)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        views.setOnClickFillInIntent(R.id.overlay_button, intent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return week.hours[firstIndex + position].id.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getCount(): Int {
        return if (isEmpty)
            0
        else
            max(0, lastIndex + 1 - firstIndex)
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun onDestroy() {
    }

}