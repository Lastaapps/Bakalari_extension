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

package cz.lastaapps.bakalari.app.ui.user.timetable.small

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalari.api.core.homework.holders.HomeworkList
import cz.lastaapps.bakalari.api.core.timetable.holders.Week
import cz.lastaapps.bakalari.api.core.user.holders.User
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import cz.lastaapps.bakalari.settings.R
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCzechDate
import cz.lastaapps.bakalari.tools.TimeTools.toCzechZoned
import cz.lastaapps.bakalari.tools.TimeTools.toMonday
import cz.lastaapps.bakalari.tools.ui.EquippedViewModel
import cz.lastaapps.bakalari.tools.ui.dateToShowInsteadOfTomorrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

class SmallTimetableViewModel : EquippedViewModel() {

    companion object {
        private val TAG = SmallTimetableViewModel::class.simpleName
    }

    private val database = CurrentUser.requireDatabase()

    /**date of currently loaded week*/
    val date = MutableLiveData<LocalDate>()

    init {
        updateDate()
    }

    /**changes date for weekend*/
    private fun updateDate() {
        viewModelScope.launch(Dispatchers.Default) {
            val now = TimeTools.now

            val week = database.timetableRepository.getWeekForDay(now.toCzechDate()).first()
            if (week != null) {
                Log.i(TAG, "Date based on the timetable available")
            }

            val lastLessonTime = dateToShowInsteadOfTomorrow(
                week,
                MySettings.withAppContext().TIMETABLE_PREVIEW,
                R.array.sett_timetable_preview
            ) ?: now

            val newDate = lastLessonTime.toCzechDate()

            Log.i(TAG, "New date obtained: ${newDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
            date.postValue(newDate)
        }
    }

    val hasData = MutableLiveData<Boolean>()
    val isFailed = MutableLiveData<Boolean>()
    val isLoading = MutableLiveData<Boolean>()
    val week = MutableLiveData<Week>()
    val homeworkList = MutableLiveData<HomeworkList>()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val user = database.userRepository.getUser().first()
                ?: throw IllegalStateException("No user saved in the API database")

            val date = waitForDate() //date don't have to be initialized yet
            Log.i(TAG, "Date available")
            val monday = date.toMonday()

            val repo = database.timetableRepository.getRepositoryForDate(monday)

            val mHasData = repo.hasData.asLiveData()
            val mIsFailed = repo.isFailed.asLiveData()
            val mIsLoading = repo.isLoading.asLiveData()

            mHasData.redirect(hasData)
            mIsFailed.redirect(isFailed)
            mIsLoading.redirect(isLoading)

            withContext(Dispatchers.Main) {
                var initialized = false
                mHasData.observeForever {
                    Log.i(TAG, "Has data: $it")

                    if (it == true && !initialized) {
                        repo.getWeekForDate(date).observe { newWeek ->

                            if (newWeek != null) {
                                Log.i(TAG, "Week loaded")
                                week.value = newWeek
                            }
                        }

                        if (user.isModuleEnabled(User.HOMEWORK)) {
                            Log.i(TAG, "Homework available")
                            val zonedDate = date.toCzechZoned()

                            database.homeworkRepository.getHomeworkListForDates(
                                zonedDate,
                                zonedDate.plusDays(1).minusSeconds(1)
                            ).observe {

                                Log.i(TAG, "Homework list loaded")
                                homeworkList.value = it
                            }
                        }

                        initialized = true
                    }
                }
            }
        }
    }

    private suspend fun <T> LiveData<T>.redirect(target: MutableLiveData<T>) {
        withContext(Dispatchers.Main) {
            observeForever {
                target.value = it
            }
        }
    }

    private suspend fun waitForDate(): LocalDate =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<LocalDate> { continuation ->
                val observer: ((LocalDate) -> Unit) = { newDate: LocalDate ->
                    continuation.resume(newDate)
                }
                date.observeForever(observer)

                continuation.invokeOnCancellation {
                    date.removeObserver(observer)
                }
            }
        }

}