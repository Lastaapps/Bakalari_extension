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

package cz.lastaapps.bakalari.app.ui.user.timetable

import android.content.Context
import androidx.collection.LruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalari.app.api.timetable.WebTimetableDate
import cz.lastaapps.bakalari.app.api.timetable.WebTimetableType
import cz.lastaapps.bakalari.app.api.user.data.User
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import cz.lastaapps.bakalari.tools.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.resume

/**Contains data for */
class TimetableMainViewModel : ViewModel() {

    companion object {
        private const val TIMETABLE_CACHE = 8 * 1024 * 1024
    }

    private val database = CurrentUser.requireDatabase()
    private val repo = database.timetableRepository

    lateinit var user: User

    /**current date for normal timetable*/
    lateinit var selectedDate: LocalDate
    lateinit var rangeMin: LocalDate
    lateinit var rangeMax: LocalDate
    private val initialized = MutableLiveData(false)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            user = database.userRepository.getUser().first()
                ?: throw IllegalStateException("User is not contained in the database")

            initSelectedDate(user)

            rangeMin = user.firstSeptember //1.9.
            rangeMax = rangeMin.plusYears(1).minusDays(1) //31.8.

            initialized.postValue(true)
        }
    }

    private fun initSelectedDate(user: User) = synchronized(this) {
        if (!this::selectedDate.isInitialized) {
            val today = TimeTools.monday.toLocalDate()
            val required = user.firstSeptember

            val selected = if (today > required) today else required
            selectedDate =
                selected.plusDays(MySettings.withAppContext().getTimetableDayOffset().toLong())
        }
    }

    /**waits until user related values are initialized*/
    suspend fun waitForInitialization(lifecycle: Lifecycle) =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Boolean> { continuation ->
                val observer: ((Boolean) -> Unit) = {
                    continuation.resume(it)
                }
                initialized.observe({ lifecycle }, observer)

                continuation.invokeOnCancellation {
                    initialized.removeObserver(observer)
                }
            }
        }

    //if permanent timetable is shown now
    var isPermanent = MutableLiveData(false)

    //cycle index for permanent timetable
    var cycleIndex = 0

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
    fun openWebTimetable(
        context: Context,
        date: WebTimetableDate,
        type: WebTimetableType,
        id: String
    ) =
        repo.openWebTimetable(context, date, type, id)
}