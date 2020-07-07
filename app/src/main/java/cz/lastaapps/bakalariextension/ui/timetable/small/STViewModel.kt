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

package cz.lastaapps.bakalariextension.ui.timetable.small

import android.content.Context
import androidx.lifecycle.MutableLiveData
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.RefreshableViewModel
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**Holds data for SmallTimetableFragment*/
class STViewModel : RefreshableViewModel<Week>(TAG) {

    companion object {
        private val TAG = STViewModel::class.java.simpleName
    }

    /**date of currently loaded week*/
    val date = MutableLiveData(defaultDate())

    /**changes date for weekend*/
    private fun defaultDate(): ZonedDateTime {
        val now = TimeTools.now

        return if (arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                .contains(now.dayOfWeek)
        ) {
            MySettings.withAppContext().run {
                showTomorrow(now, now, TIMETABLE_PREVIEW, R.array.sett_timetable_preview)
            }
        } else
            now
    }

    /**Timetable data in for of Week object*/
    val week = data

    override suspend fun loadServer(): Week? {
        return TimetableLoader.loadFromServer(date.value!!)
    }

    override suspend fun loadStorage(): Week? {
        return TimetableLoader.loadFromStorage(date.value!!)
    }

    override fun shouldReload(): Boolean {
        return TimetableLoader.shouldReload(date.value!!)
    }

    override fun isEmpty(data: Week): Boolean {
        return data.hasValidDays()
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.timetable_no_timetable)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.timetable_failed_to_load)
    }
}