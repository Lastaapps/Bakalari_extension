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
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViewsService
import cz.lastaapps.bakalari.api.entity.timetable.Day
import cz.lastaapps.bakalari.api.entity.timetable.Week
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.LocaleManager
import cz.lastaapps.bakalari.settings.MySettings
import cz.lastaapps.bakalari.tools.toSerializable

class SmallTimetableRemoteService : RemoteViewsService() {

    companion object {
        private val TAG = SmallTimetableRemoteService::class.java.simpleName

        //date for witch should be data loaded
        const val WEEK_EXTRA = "WEEK_EXTRA"
        const val DAY_EXTRA = "DAY_EXTRA"
    }

    override fun attachBaseContext(newBase: Context) {
        //updates language
        super.attachBaseContext(LocaleManager.updateLocale(newBase, MySettings.withAppContext()))
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {

        Log.i(TAG, "Creating view factory")

        //gets data from widget
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)

        val week = intent.getByteArrayExtra(WEEK_EXTRA)!!.toSerializable<Week>()
        val day = intent.getByteArrayExtra(DAY_EXTRA)!!.toSerializable<Day>()

        //creates Factory for Timetable cells
        return SmallTimetableRemoteViewsFactory(
            this,
            widgetId, week, day
        )
    }
}
