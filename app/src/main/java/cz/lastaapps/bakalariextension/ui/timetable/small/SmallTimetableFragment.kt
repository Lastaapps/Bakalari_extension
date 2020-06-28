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
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.UserViewModel
import cz.lastaapps.bakalariextension.ui.homework.HmwViewModel
import java.time.ZonedDateTime

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
        //gets date to load final timetable
        if (vm.week.value != null) {
            val oldDate = vm.date.value!!

            val hours = vm.week.value!!.hours
            val today = vm.week.value!!.today()

            //in the evening tomorrows timetable can be loaded
            vm.date.value = MySettings(requireContext()).showTomorrowsPreview(
                TimeTools.now,

                if (today != null) {
                    val index = today.lastLessonIndex(hours, null)
                    if (index >= 0) {
                        //for normal days, gets end of the last lesson
                        val hour = hours[index]
                        val endTime =
                            TimeTools.parseTime(
                                hour.end,
                                TimeTools.TIME_FORMAT,
                                TimeTools.CET
                            )

                        ZonedDateTime.of(
                            TimeTools.today.toLocalDate(),
                            endTime,
                            TimeTools.CET
                        )
                    } else {
                        //for holidays and empty days
                        TimeTools.today.withHour(14)
                    }
                } else {
                    //for weekend, returns next Monday
                    TimeTools.now
                }
            )

            if (TimeTools.toMonday(oldDate).toLocalDate()
                != TimeTools.toMonday(vm.date.value!!).toLocalDate()
            ) {
                vm.onRefresh()
                return true
            }
        }
        return false
    }

    private fun onSuccess() {
        if (!initDate()) {

            Log.i(TAG, "Showing data")

            //updates data
            val week = vm.week.value!!
            val date = vm.date.value!!
            val homework = vmHomework.homework.value

            val day = week.getDay(date)
            if (day == null) {
                view.setError(resources.getString(R.string.timetable_no_timetable_today))
            } else {
                view.updateTimetable(week, day, userViewModel.requireData(), homework)
            }
        }
    }

    private fun onFail() {
        view.setError(resources.getString(R.string.timetable_no_internet))
    }
}
