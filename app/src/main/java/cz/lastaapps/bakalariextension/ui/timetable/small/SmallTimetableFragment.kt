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
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.validDate
import cz.lastaapps.bakalariextension.ui.UserViewModel
import cz.lastaapps.bakalariextension.ui.homework.HmwViewModel

class SmallTimetableFragment : Fragment() {

    companion object {
        private val TAG = SmallTimetableFragment::class.java.simpleName
    }

    private lateinit var view: SmallTimetableView
    private val vm: STViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val vmHomework: HmwViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        view = SmallTimetableView(inflater.context)
        vm.isRefreshing.observe({ lifecycle }) { state: Boolean ->
            if (state) {
                view.setLoading()
            }
        }

        view.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.nav_timetable)
        }

        //sets date and observes for updates
        vm.date.value?.let { view.setDate(it) }
        vm.date.observe({ lifecycle }) {
            Log.i(TAG, "Date updated to ${TimeTools.format(it, TimeTools.COMPLETE_FORMAT)}")
            view.setDate(it)
        }

        //observes for data changes
        vm.failed.observe({ lifecycle }) {
            if (it) {
                Log.i(TAG, "Failed to load")
                onFail()
            }
        }

        //loads the timetable if it wasn't already done
        vm.executeOrRefresh(lifecycle) {
            Log.i(TAG, "Data loaded")
            onSuccess()
        }

        //loads the homework list if it wasn't already done
        vmHomework.executeOrRefresh(lifecycle) {
            Log.i(TAG, "Homework list loaded")
            onSuccess()
        }

        return view
    }

    /**
     * Gets date to load final timetable and saves it to ViewModel
     * @return if request for other Timetable was made
     */
    private fun initDate(): Boolean {

        val oldDate = vm.date.value!!
        val newDate = validDate(
            vm.week.value,
            MySettings.withAppContext().TIMETABLE_PREVIEW,
            R.array.sett_timetable_preview
        ) ?: return false

        return if (TimeTools.toMonday(oldDate).toLocalDate()
            != TimeTools.toMonday(newDate).toLocalDate()
        ) {
            vm.date.value = newDate
            vm.onRefresh()
            true
        } else {
            false
        }
    }

    private fun onSuccess() {
        if (!initDate()) {

            Log.i(TAG, "Showing data")

            //updates data
            val week = vm.week.value ?: return
            val date = vm.date.value!!
            val homework = vmHomework.homework.value

            val day = week.getDay(date)
            if (day == null) {
                view.setError(resources.getString(R.string.timetable_no_timetable))
            } else {
                view.updateTimetable(week, day, userViewModel.requireData(), homework)
            }
        }
    }

    private fun onFail() {
        view.setError(resources.getString(R.string.timetable_failed_to_load))
    }
}
