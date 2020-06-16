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
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TimetableLoader
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.RefreshableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**Holds data for SmallTimetableFragment*/
class STViewModel : RefreshableViewModel<Week>() {
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
            MySettings.withAppContext().showTomorrowsPreview(now, now)
        } else
            now
    }

    /**Timetable data in for of Week object*/
    val week = data

    /**Loads timetable for the date in the variable #date*/
    override fun onRefresh(force: Boolean) {

        if (isRefreshing.value == true)
            return

        //set state refreshing
        isRefreshing.value = true

        viewModelScope.launch(Dispatchers.Default) {
            Log.i(TAG, "Loading timetable")

            var loaded: Week?

            if (force) {
                loaded = TimetableLoader.loadFromServer(date.value!!)
            } else {

                loaded = TimetableLoader.loadFromStorage(date.value!!)

                if (TimetableLoader.shouldReload(date.value!!) || loaded == null) {
                    loaded?.let {
                        withContext(Dispatchers.Main) {
                            week.value = it
                        }
                    }

                    //let oder work finish before running slow loading from server
                    for (i in 0 until 10) yield()

                    loaded = TimetableLoader.loadFromServer(date.value!!)
                }
            }

            withContext(Dispatchers.Main) {

                //hides refreshing icon
                failed.value = false

                //when download failed
                if (loaded == null) {
                    Log.e(TAG, "Loading failed")
                    Toast.makeText(
                        App.context, R.string.error_cannot_download_timetable, Toast.LENGTH_LONG
                    ).show()

                    failed.value = true
                } else {
                    //updates marks with new value
                    week.value = loaded
                }

                isRefreshing.value = false
            }
        }
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.error_no_timetable_for_today)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.error_cannot_download_timetable)
    }
}