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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViewsService
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.LocaleManager
import cz.lastaapps.bakalariextension.tools.TimeTools
import java.time.Instant
import java.time.ZonedDateTime

class SmallTimetableRemoteAdapterService : RemoteViewsService() {

    companion object {
        private val TAG = SmallTimetableRemoteAdapterService::class.java.simpleName

        //date for witch should be data loaded
        const val DATE_EXTRA = "DATE_EXTRA"
        const val LOAD_DEFAULT_EXTRA = "LOAD_DEFAULT_EXTRA"
    }

    override fun attachBaseContext(newBase: Context) {
        //updates language
        super.attachBaseContext(LocaleManager.updateLocale(newBase))
    }

    private var factoryCache: FactoryCache? = null


    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {

        Log.i(TAG, "Creating view factory")

        //gets data from widget
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
        val date = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(
                intent.getLongExtra(DATE_EXTRA, System.currentTimeMillis())
            ), TimeTools.UTC
        )
        val loadDefault = intent.getBooleanExtra(LOAD_DEFAULT_EXTRA, false)

        if (factoryCache == null)
            factoryCache = FactoryCache()

        //creates Factory for Timetable cells
        return SmallTimetableRemoteViewsFactory(
            this,
            widgetId,
            date,
            loadDefault,
            factoryCache!!
        )
    }

    /**caches current weeks timetable for faster loading*/
    class FactoryCache {
        var currentWeek: Week? = null
    }
}
