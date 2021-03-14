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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.widgets.WidgetConfigure
import cz.lastaapps.bakalari.platform.App

class Widget(
    private val context: Context,
    private val widgetId: Int,
    val views: RemoteViews
) {

    fun setTheme(updater: WidgetConfigure.Updater) {
        //changes background
        val background = updater.applyAlpha(
            widgetId, if (updater.isLight(widgetId))
                R.color.widget_background
            else
                R.color.widget_background_dark
        )
        val foreground = App.getColor(
            if (updater.isLight(widgetId))
                R.color.widget_foreground
            else
                R.color.widget_foreground_dark
        )

        views.setInt(R.id.widget_root, "setBackgroundColor", background)

        //changes text color
        views.setTextColor(R.id.error_message, foreground)
        views.setTextColor(R.id.date_label, foreground)
        views.setTextColor(R.id.holiday, foreground)

        views.setViewVisibility(R.id.error_message, View.GONE)
        views.setViewVisibility(R.id.holiday, View.GONE)
        views.setViewVisibility(R.id.grid_view, View.GONE)
    }

    fun addIntents() {
        //opens full timetable
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(MainActivity.NAVIGATE, R.id.nav_timetable)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setPendingIntentTemplate(R.id.grid_view, pendingIntent)

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

    }
}