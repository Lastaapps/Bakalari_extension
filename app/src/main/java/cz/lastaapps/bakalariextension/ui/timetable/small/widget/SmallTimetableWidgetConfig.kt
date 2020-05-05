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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.WidgetConfigure


/**The configuration activity or small timetable widget*/
class SmallTimetableWidgetConfig : WidgetConfigure(smallTimetableWidgetConfig) {
    companion object {
        val smallTimetableWidgetConfig = object : WidgetConfigurePreferences {
            override fun getSPKey(): String = "timetable_small_widget"
            override fun getPrefix(): String = "appwidget_"
            override fun getLayoutId(): Int = R.layout.widget_small_timetable
            override fun updateRemoteViews(
                remoteViews: RemoteViews,
                widgetId: Int,
                context: Context
            ) {
                //updates styles of the remote views
                SmallTimetableWidget.setupWidget(remoteViews, widgetId, context)
            }

            override fun updateAppWidget(
                context: Context,
                manager: AppWidgetManager,
                widgetId: Int
            ) {
                //updates widget
                SmallTimetableWidget.updateAppWidget(context, manager, widgetId)
            }
        }

        val updater: Updater
            get() = Updater(smallTimetableWidgetConfig)
    }
}