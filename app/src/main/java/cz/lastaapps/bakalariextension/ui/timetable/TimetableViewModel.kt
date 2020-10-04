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
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.CurrentUser
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.timetable.TimetableRepository
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.ui.RefreshableDataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class TimetableViewModel(date: LocalDate) : RefreshableDataViewModel<Week, TimetableRepository>(
    TAG,
    CurrentUser.requireDatabase().timetableRepository.getRepositoryForDate(date)
) {

    companion object {
        private val TAG = TimetableViewModel::class.java.simpleName
    }

    override val data by lazy {
        runBlocking(Dispatchers.Default) {
            repo.getWeek().filterNotNull().asLiveData()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Main) {
            data.observeForever { week ->
                val validHours = week.trimFreeMorning()
                isEmpty.value = validHours.isEmpty() || !week.hasValidDays()
            }
        }
    }

    override fun emptyText(context: Context): String = context.getString(R.string.timetable_empty)

    override fun failedText(context: Context): String =
        context.getString(R.string.timetable_failed_to_load)
}