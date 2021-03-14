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
import android.widget.RemoteViews
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.widgets.WidgetConfigure
import cz.lastaapps.bakalari.platform.App

class WidgetTextOnly(
    private val context: Context,
    private val widgetId: Int,
    val text: String,
    val views: RemoteViews
) {

    init {
        views.setTextViewText(R.id.text, text)
    }

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
        views.setTextColor(R.id.label, foreground)
    }
}