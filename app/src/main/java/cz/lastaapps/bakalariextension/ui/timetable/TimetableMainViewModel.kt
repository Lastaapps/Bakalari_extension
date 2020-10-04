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

package cz.lastaapps.bakalariextension.ui.timetable

import android.content.Context
import androidx.collection.LruCache
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.CurrentUser
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.user.data.User
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toCzechDate
import java.time.DayOfWeek
import java.time.LocalDate

/**Contains data for */
class TimetableMainViewModel : ViewModel() {

    companion object {
        private const val TIMETABLE_CACHE = 8 * 1024 * 1024
    }

    private val database = CurrentUser.requireDatabase()
    private val repo = database.timetableRepository

    //current date for normal timetable
    lateinit var selectedDate: LocalDate

    fun initSelectedDate(user: User) = synchronized(this) {
        if (!this::selectedDate.isInitialized) {
            val today = TimeTools.monday.toLocalDate()
            val required = user.firstSeptember

            val selected = if (today > required) today else required
            selectedDate =
                selected.plusDays(MySettings(App.context).getTimetableDayOffset().toLong())
        }
    }

    //if permanent timetable is shown now
    var isPermanent = MutableLiveData(false)

    //cycle index for permanent timetable
    var cycleIndex = 0

    /**date of currently loaded week*/
    val shownDate = MutableLiveData(defaultDate())

    /**changes date for weekend*/
    private fun defaultDate(): LocalDate {
        val now = TimeTools.now

        return if (arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                .contains(now.dayOfWeek)
        ) {
            MySettings.withAppContext().run {
                showTomorrow(now, now, TIMETABLE_PREVIEW, R.array.sett_timetable_preview)
            }
        } else {
            now
        }
            .toCzechDate()
    }

    private val cache by lazy { LruCache<LocalDate, TimetableViewModel>(TIMETABLE_CACHE) }
    fun getTimetableViewModel(date: LocalDate): TimetableViewModel {
        synchronized(this) {
            var value = cache.get(date)
            if (value == null) {
                value = TimetableViewModel(date)
                cache.put(date, value)
            }
            return value
        }
    }

    private var isAvailable = false
    suspend fun isWebTimetableAvailable(): Boolean {
        return if (!isAvailable) {
            isAvailable = repo.isWebTimetableAvailable()
            isAvailable
        } else
            true
    }

    fun openWebTimetable(context: Context) = repo.openWebTimetable(context)
    fun openWebTimetable(context: Context, date: String, type: String, id: String) =
        repo.openWebTimetable(context, date, type, id)
}