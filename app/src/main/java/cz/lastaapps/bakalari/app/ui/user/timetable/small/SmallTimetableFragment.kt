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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.user.UserViewModel
import java.time.format.DateTimeFormatter

class SmallTimetableFragment : Fragment() {

    companion object {
        private val TAG = SmallTimetableFragment::class.java.simpleName
    }

    private lateinit var view: SmallTimetableView

    private val smallViewModel: SmallTimetableViewModel by accountsViewModels()
    private val userViewModel: UserViewModel by accountsViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "Creating view")

        view = SmallTimetableView(inflater.context)

        view.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.nav_timetable)
        }

        //sets date and observes for updates
        smallViewModel.apply {
            date.observe({ lifecycle }) {
                Log.i(TAG, "Date updated to ${it.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                view.setDate(it)
            }

            isLoading.observe({ lifecycle }) {
                if (it == true) view.setLoading()
            }

            isFailed.observe({ lifecycle }) {
                if (it == true) view.setError(getString(R.string.timetable_failed_to_load))
            }

            week.observe({ lifecycle }) {
                onSuccess()
            }

            homeworkList.observe({ lifecycle }) {
                onSuccess()
            }
        }

        userViewModel.data.observe({ lifecycle }) {
            onSuccess()
        }

        return view
    }

    private fun onSuccess() {
        //updates data
        val week = smallViewModel.week.value ?: return
        val date = smallViewModel.date.value ?: return
        val homework = smallViewModel.homeworkList.value
        val user = userViewModel.data.value ?: return

        Log.i(TAG, "Showing data for date " + date.format(DateTimeFormatter.ISO_LOCAL_DATE))

        val day = week.getDay(date)
        if (day == null) {
            view.setError(resources.getString(R.string.timetable_no_timetable))
        } else {
            view.updateTimetable(week, day, user, homework)
        }
    }

/*
    /**
     * Gets date to load final timetable and saves it to ViewModel
     * @return if request for other Timetable was made
     */
    private fun initDate(): Boolean {

        val oldDate = smallViewModel.date.value!!
        val newDate = dateToShowInsteadOfTomorrow(
            currentViewModel.data.value,
            MySettings.withAppContext().TIMETABLE_PREVIEW,
            R.array.sett_timetable_preview
        )?.toCzechDate() ?: return false

        return if (oldDate.toMonday() != newDate.toMonday()) {
            smallViewModel.date.value = newDate
            setCurrentViewModel(mainViewModel.getTimetableViewModel(newDate))
            true
        } else {
            false
        }
    }

    private fun setCurrentViewModel(current: TimetableViewModel) {

        if (this@SmallTimetableFragment::currentViewModel.isInitialized) {
            currentViewModel.isLoading.removeObservers { lifecycle }
            currentViewModel.isFailed.removeObservers { lifecycle }
            currentViewModel.data.removeObservers { lifecycle }
        }

        currentViewModel = current

        currentViewModel.isLoading.observe({ lifecycle }) { state: Boolean ->
            if (state) {
                view.setLoading()
            }
        }

        //observes for data changes
        currentViewModel.isFailed.observe({ lifecycle }) { state: Boolean ->
            if (state) {
                Log.i(TAG, "Failed to load")
                view.setError(resources.getString(R.string.timetable_failed_to_load))
            }
        }

        //loads the timetable if it wasn't already done
        currentViewModel.runOrRefresh(lifecycle) {
            Log.i(TAG, "Data loaded")
            onSuccess()
        }
    }*/
}
