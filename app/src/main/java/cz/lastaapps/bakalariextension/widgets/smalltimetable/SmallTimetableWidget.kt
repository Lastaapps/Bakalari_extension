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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.api.timetable.data.Day
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime


/**
 * Implementation of App Widget functionality.
 */
class SmallTimetableWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i(TAG, "Updating widgets")

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId
            )
        }
    }

    override fun onEnabled(context: Context) {}

    override fun onDisabled(context: Context) {}

    companion object {

        private val TAG = SmallTimetableWidget::class.java.simpleName

        /**Updates SmallTimetable widgets*/
        fun update(context: Context) {
            val intent = Intent(context, SmallTimetableWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

            val ids: IntArray = AppWidgetManager.getInstance(App.app)
                .getAppWidgetIds(
                    ComponentName(context, SmallTimetableWidget::class.java)
                )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_small_timetable)

            setupWidget(
                views,
                widgetId,
                context
            )

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /**sets up remote view*/
        fun setupWidget(
            views: RemoteViews,
            widgetId: Int,
            context: Context,
            useDefault: Boolean = false
        ) {

            val config = SmallTimetableWidgetConfig.updater

            //changes background
            val background = config.applyAlpha(
                widgetId, if (config.isLight(widgetId))
                    R.color.widget_background
                else
                    R.color.widget_background_dark
            )
            val foreground = App.getColor(
                if (config.isLight(widgetId))
                    R.color.widget_foreground
                else
                    R.color.widget_foreground_dark
            )
            views.setInt(R.id.widget_root, "setBackgroundColor", background)

            //changes text color
            views.setTextColor(R.id.error_message, foreground)
            views.setTextColor(R.id.date_label, foreground)

            //loads week
            val date = TimeTools.today
            val week: Week?
            val day: Day?

            views.setTextViewText(R.id.date_label, TimeTools.format(date, "E, d. MMMM"))

            if (!useDefault) {
                //week is not downloaded yet
                week = loadTimetable(date)
                if (week == null) {
                    views.setViewVisibility(R.id.error_message, View.VISIBLE)
                    views.setViewVisibility(R.id.grid_view, View.GONE)
                    return
                }
                day = week.getDay(date)
                //on weekend is null
                if (day == null) {
                    views.setViewVisibility(R.id.error_message, View.VISIBLE)
                    views.setViewVisibility(R.id.grid_view, View.GONE)
                    return
                }
            } else {
                week = null
                day = null
            }

            //opens full timetable
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(MainActivity.NAVIGATE, R.id.nav_timetable)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.grid_view, pendingIntent)

            views.setEmptyView(R.id.grid_view, R.id.error_message)

            //differs for holiday and workday
            if (useDefault || (day != null && !day.isHoliday())) {

                //intent to start service providing data for widget
                val gridViewsServiceIntent =
                    Intent(context, SmallTimetableRemoteAdapterService::class.java)
                //puts in widget id
                gridViewsServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

                //puts in today's date
                gridViewsServiceIntent.putExtra(
                    SmallTimetableRemoteAdapterService.DATE_EXTRA,
                    date.toInstant().toEpochMilli()
                )

                //if universal timetable should be loaded
                gridViewsServiceIntent.putExtra(
                    SmallTimetableRemoteAdapterService.LOAD_DEFAULT_EXTRA,
                    useDefault
                )

                gridViewsServiceIntent.data = Uri.parse(
                    gridViewsServiceIntent.toUri(Intent.URI_INTENT_SCHEME)
                )

                //adds remote adapter service
                views.setViewVisibility(R.id.grid_view, View.VISIBLE)
                views.setRemoteAdapter(R.id.grid_view, gridViewsServiceIntent)

            } else {

                //sets up holiday view
                views.setTextViewText(R.id.holiday, day!!.getHolidayDescription())
                views.setViewVisibility(R.id.cell_main, View.VISIBLE)
            }
        }

        /**loads timetable using coroutines*/
        private fun loadTimetable(date: ZonedDateTime): Week? {
            var coroutineRunning = true

            var week: Week? = null
            CoroutineScope(Dispatchers.Default).launch {
                week = TimetableLoader.loadFromStorage(TimeTools.toMonday(date))

                coroutineRunning = false
            }

            while (coroutineRunning)
                Thread.sleep(1)

            return week
        }
    }
}
