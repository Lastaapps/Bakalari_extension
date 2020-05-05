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

package cz.lastaapps.bakalariextension.ui.timetable.small.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.LoadingActivity
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.Timetable
import cz.lastaapps.bakalariextension.tools.TimeTools


/**
 * Implementation of App Widget functionality.
 */
class SmallTimetableWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_small_timetable)

            setupWidget(views, widgetId, context)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /**sets up remote view*/
        fun setupWidget(views: RemoteViews, widgetId: Int, context: Context) {

            val config = SmallTimetableWidgetConfig.updater

            //changes background
            val background = config.applyAlpha(
                widgetId, if (config.isLight(widgetId))
                    R.color.widget_background
                else
                    R.color.widget_background_dark
            )
            views.setInt(R.id.widget_root, "setBackgroundColor", background)

            //changes error text color
            views.setTextColor(
                R.id.error_message, App.getColor(
                    if (config.isLight(widgetId))
                        R.color.widget_foreground
                    else
                        R.color.widget_foreground_dark
                )
            )

            //opens full timetable
            val intent = Intent(context, LoadingActivity::class.java)
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

            //loads week
            val week = Timetable.loadFromStorage(TimeTools.monday)

            //week is not downloaded yet
            if (week == null) {
                views.setViewVisibility(R.id.error_message, View.VISIBLE)
                return
            }
            val day = week.today()
            //on weekend is null
            if (day == null) {
                views.setViewVisibility(R.id.error_message, View.VISIBLE)
                return
            }

            views.setEmptyView(R.id.grid_view, R.id.error_message)

            //differs for holiday and workday
            if (!day.isHoliday()) {

                //intent to start service providing data for widget
                val gridViewsServiceIntent =
                    Intent(context, SmallTimetableRemoteAdapterService::class.java)
                //puts in widget id
                gridViewsServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                //puts in today's date
                gridViewsServiceIntent.putExtra(
                    SmallTimetableRemoteAdapterService.DATE_EXTRA,
                    TimeTools.now.toInstant().toEpochMilli()
                )
                gridViewsServiceIntent.data = Uri.parse(
                    gridViewsServiceIntent.toUri(Intent.URI_INTENT_SCHEME)
                )

                //adds remote adapter service
                views.setViewVisibility(R.id.grid_view, View.VISIBLE)
                views.setRemoteAdapter(R.id.grid_view, gridViewsServiceIntent)

            } else {

                //sets up holiday view
                views.setTextViewText(R.id.holiday, day.getHolidayDescription())
                views.setViewVisibility(R.id.cell_main, View.VISIBLE)
            }
        }
    }
}
