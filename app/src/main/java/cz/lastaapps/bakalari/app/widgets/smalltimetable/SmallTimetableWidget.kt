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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import cz.lastaapps.bakalari.api.core.timetable.holders.Day
import cz.lastaapps.bakalari.api.core.timetable.holders.Week
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.platform.App
import cz.lastaapps.bakalari.tools.toBytes
import java.time.LocalDate
import java.time.format.DateTimeFormatter


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

        WidgetData.getAll().values.forEach {
            it.updateDate()
        }

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateWidget(
                context,
                appWidgetManager,
                appWidgetId
            )
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        WidgetData.removeObservers(ids.toList())
    }

    override fun onEnabled(context: Context) {}

    override fun onDisabled(context: Context) {}

    companion object {

        private val TAG = SmallTimetableWidget::class.java.simpleName

        /**Updates SmallTimetable widgets*/
        fun update(context: Context) {
            val ids: IntArray = AppWidgetManager.getInstance(App.app)
                .getAppWidgetIds(ComponentName(context, SmallTimetableWidget::class.java))

            updateIds(context, ids)
        }

        /**Updates SmallTimetable widgets*/
        fun updateIds(context: Context, ids: IntArray) {
            val intent = Intent(context, SmallTimetableWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val userId =
                SmallTimetableWidgetConfig.updater.run { getPrefString(USER_UUID, widgetId) }

            //crashes when using the UUID of removed user
            val widgetData = try {
                if (userId == "") throw IllegalArgumentException("Invalid user id")
                WidgetData.getWidgetData(context, userId)
            } catch (e: Exception) {
                e.printStackTrace()

                val views =
                    setUpTextOnly(widgetId, context, R.string.widget_small_timetable_user_removed)

                appWidgetManager.updateAppWidget(widgetId, views)

                return
            }
            widgetData.registerObserver(widgetId)
            val week = widgetData.getWeek()
            val date = LocalDate.now()
            val day = week?.getDay(date)

            val views =
                if (!widgetData.hasData()) {

                    setUpTextOnly(widgetId, context, R.string.widget_small_timetable_loading)

                } else {

                    setupWidget(
                        RemoteViews(context.packageName, R.layout.widget_small_timetable),
                        widgetId,
                        context,
                        week,
                        date,
                        day,
                    )
                }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun setUpTextOnly(
            widgetId: Int,
            context: Context,
            @StringRes text: Int,
        ): RemoteViews {

            val views = RemoteViews(context.packageName, R.layout.widget_small_timetable_text_only)
            val widget = WidgetTextOnly(context, widgetId, context.getString(text), views)

            widget.setTheme(SmallTimetableWidgetConfig.updater)

            return views
        }

        /**sets up remote view*/
        fun setupWidget(
            views: RemoteViews,
            widgetId: Int,
            context: Context,
            week: Week?,
            date: LocalDate = LocalDate.now(),
            day: Day? = week?.getDay(date),
        ): RemoteViews {

            val widget = Widget(context, widgetId, views)
            val updater = SmallTimetableWidgetConfig.updater

            //sets widget theme
            widget.setTheme(updater)

            widget.addIntents()

            //updates date text
            views.setTextViewText(
                R.id.date_label,
                date.format(DateTimeFormatter.ofPattern("E, d. MMMM"))
            )


            if (week == null) {
                views.setViewVisibility(R.id.error_message, View.VISIBLE)
                return views
            }

            //on weekend is null
            if (day == null) {
                views.setViewVisibility(R.id.error_message, View.VISIBLE)
                return views
            }


            //differs for holiday and workday
            if (!day.isHoliday()) {

                //intent to start service providing data for widget
                val intent =
                    Intent(context, SmallTimetableRemoteService::class.java)

                //puts in widget id
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

                //puts in today's date
                intent.putExtra(
                    SmallTimetableRemoteService.WEEK_EXTRA,
                    week.toBytes()
                )

                //if universal timetable should be loaded
                intent.putExtra(
                    SmallTimetableRemoteService.DAY_EXTRA,
                    day.toBytes()
                )

                intent.data = Uri.parse(
                    intent.toUri(Intent.URI_INTENT_SCHEME)
                )


                //adds remote adapter service
                views.setViewVisibility(R.id.grid_view, View.VISIBLE)
                views.setRemoteAdapter(R.id.grid_view, intent)

            } else {

                //sets up holiday view
                views.setTextViewText(R.id.holiday, day.getHolidayDescription(context))
                views.setViewVisibility(R.id.holiday, View.VISIBLE)
            }

            return widget.views
        }
    }
}
