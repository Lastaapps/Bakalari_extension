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

package cz.lastaapps.bakalariextension.ui.absence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.CurrentUser
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.absence.AbsenceRepository
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceDay
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceMonth
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceRoot
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceSubject
import cz.lastaapps.bakalariextension.ui.RefreshableViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

private typealias MonthData = MutableLiveData<DataIdList<AbsenceMonth>>

class AbsenceViewModel : RefreshableViewModel<AbsenceRepository>(
    TAG,
    CurrentUser.requireDatabase().absenceRepository
) {

    companion object {
        private val TAG = AbsenceViewModel::class.java.simpleName
    }

    private lateinit var months: MonthData
    fun getMonths(firstSeptember: LocalDate): MonthData = synchronized(this) {
        if (!this::months.isInitialized) {
            months = MonthData()
            days.updateMonths(firstSeptember)
        }
        return months
    }

    private val mRoot = MutableLiveData<AbsenceRoot>()
    val root = mRoot.distinctUntilChanged()

    val thresholdHolder = repo.getThreshold().asLiveData().updateRoot()

    val days = repo.getDays().asLiveData().updateRoot()

    val subjects = repo.getSubjects().asLiveData().updateRoot()

    suspend fun getDay(date: ZonedDateTime): AbsenceDay? = repo.getDay(date)

    suspend fun getSubject(name: String): AbsenceSubject? = repo.getSubject(name)

    private fun <E> LiveData<E>.updateRoot(): LiveData<E> {
        viewModelScope.launch(Dispatchers.Main) {
            this@updateRoot.distinctUntilChanged().observeForever {

                try {
                    //constructor is called on worker thread, so this block can be called
                    //on the main thread before the items are initialized
                    val thr = thresholdHolder.value ?: return@observeForever
                    val days = days.value ?: return@observeForever
                    val subjects = subjects.value ?: return@observeForever

                    mRoot.value = AbsenceRoot(thr, days, subjects)

                } catch (e: Exception) {
                    return@observeForever
                }
            }
        }
        return this
    }

    private fun LiveData<DataIdList<AbsenceDay>>.updateMonths(firstSeptember: LocalDate): LiveData<DataIdList<AbsenceDay>> {

        viewModelScope.launch(Dispatchers.Main) {
            this@updateMonths.distinctUntilChanged().observeForever {
                months.value = AbsenceMonth.daysToMonths(App.context, firstSeptember, it)
            }
        }
        return this
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.absence_no_absence)
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.absence_failed_to_load)
    }
}