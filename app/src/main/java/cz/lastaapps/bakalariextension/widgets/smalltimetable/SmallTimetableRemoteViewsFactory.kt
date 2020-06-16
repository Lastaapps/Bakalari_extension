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

package cz.lastaapps.bakalariextension.widgets.smalltimetable

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.api.timetable.data.Day
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.loading.LoadingFragment
import cz.lastaapps.bakalariextension.ui.timetable.CellSetup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

/**Creates views for SmallTimetable widget*/
class SmallTimetableRemoteViewsFactory(
    val context: Context,
    val widgetId: Int,
    val date: ZonedDateTime,
    val loadDefault: Boolean,
    val cache: SmallTimetableRemoteAdapterService.FactoryCache
) : RemoteViewsService.RemoteViewsFactory {

    companion object {
        private val TAG = RemoteViewsService::class.java.simpleName
    }

    //indexes of edge lessons
    private var firstIndex = 0
    private var lastIndex = 0

    //data to work
    lateinit var week: Week
    lateinit var day: Day

    //if day is empty
    private var isEmpty = false

    override fun onCreate() {}

    /**Called after #onCreate and before #getCount*/
    override fun onDataSetChanged() {
        var coroutineRunning = true

        CoroutineScope(Dispatchers.Default).launch {
            coroutineLoading()

            coroutineRunning = false
        }

        while (coroutineRunning)
            Thread.sleep(1)
    }

    private suspend fun coroutineLoading() {

        isEmpty = false

        val day: Day?

        if (!loadDefault) {

            Log.i(TAG, "Loading " + TimeTools.format(date, TimeTools.DATE_FORMAT))

            //loads current week
            if (cache.currentWeek == null)
                cache.currentWeek = TimetableLoader.loadFromStorage(date)

            if (cache.currentWeek == null) {
                isEmpty = true
                return
            }

            week = cache.currentWeek!!

            //loads given day
            day = cache.currentWeek!!.getDay(date)
            if (day == null) {
                isEmpty = true
                return
            }
        } else {
            Log.i(TAG, "Loading default")

            //loads default timetable as preview
            week = TimetableLoader.loadDefault(context)
            day = week.days[0]
        }

        this.day = day

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