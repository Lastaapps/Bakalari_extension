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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.user.data.User
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toCzechDate
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toMonday
import cz.lastaapps.bakalariextension.tools.validDate
import cz.lastaapps.bakalariextension.ui.UserViewModel
import cz.lastaapps.bakalariextension.ui.homework.HmwViewModel
import cz.lastaapps.bakalariextension.ui.timetable.TimetableMainViewModel
import cz.lastaapps.bakalariextension.ui.timetable.TimetableViewModel
import java.time.format.DateTimeFormatter

class SmallTimetableFragment : Fragment() {

    companion object {
        private val TAG = SmallTimetableFragment::class.java.simpleName
    }

    private lateinit var view: SmallTimetableView
    private val mainViewModel: TimetableMainViewModel by activityViewModels()
    private lateinit var currentViewModel: TimetableViewModel
    private val userViewModel: UserViewModel by activityViewModels()
    private val vmHomework: HmwViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        view = SmallTimetableView(inflater.context)

        setCurrentViewModel(mainViewModel.getTimetableViewModel(mainViewModel.shownDate.value!!))

        view.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.nav_timetable)
        }

        //sets date and observes for updates
        mainViewModel.shownDate.observe({ lifecycle }) {
            Log.i(TAG, "Date updated to ${it.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
            view.setDate(it)
        }

        userViewModel.data.observe({ lifecycle }) {
            it.runIfModuleEnabled(User.HOMEWORK) {
                //loads the homework list if it wasn't already done
                vmHomework.runOrRefresh(vmHomework.homework, lifecycle) {
                    Log.i(TAG, "Homework list loaded")
                    onSuccess()
                }
            }
        }

        return view
    }

    /**
     * Gets date to load final timetable and saves it to ViewModel
     * @return if request for other Timetable was made
     */
    private fun initDate(): Boolean {

        val oldDate = mainViewModel.shownDate.value!!
        val newDate = validDate(
            currentViewModel.data.value,
            MySettings.withAppContext().TIMETABLE_PREVIEW,
            R.array.sett_timetable_preview
        )?.toCzechDate() ?: return false

        return if (oldDate.toMonday() != newDate.toMonday()) {
            mainViewModel.shownDate.value = newDate
            setCurrentViewModel(mainViewModel.getTimetableViewModel(newDate))
            true
        } else {
            false
        }
    }

    private fun onSuccess() {
        if (!initDate()) {

            //updates data
            val week = currentViewModel.data.value ?: return
            val date = mainViewModel.shownDate.value!!
            val homework = vmHomework.homework.value

            Log.i(TAG, "Showing data for date " + date.format(DateTimeFormatter.ISO_LOCAL_DATE))

            val day = week.getDay(date)
            if (day == null) {
                view.setError(resources.getString(R.string.timetable_no_timetable))
            } else {
                view.updateTimetable(week, day, userViewModel.requireData(), homework)
            }
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
    }
}
