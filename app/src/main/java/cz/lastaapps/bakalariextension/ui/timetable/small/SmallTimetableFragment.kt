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
import cz.lastaapps.bakalariextension.ui.homework.HmwViewModel
import java.time.ZonedDateTime

class SmallTimetableFragment : Fragment() {

    companion object {
        private val TAG = SmallTimetableFragment::class.java.simpleName
    }

    private lateinit var view: SmallTimetableView
    private val vm: STViewModel by activityViewModels()
    private val vmHomework: HmwViewModel by activityViewModels()

    /**Called when timetable or homework list was successfully loaded*/
    private val onSuccess = { _: Any? ->
        onSuccess()
    }

    /**Called when timetable cannot be loaded*/
    private val onFail = { failed: Boolean ->
        if (failed)
            onFail()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //observes for data changes
        vm.week.observe({ lifecycle }, onSuccess)
        vmHomework.homework.observe({ lifecycle }, onSuccess)
        vm.failed.observe({ lifecycle }, onFail)
    }

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

        //loads the timetable if it wasn't already done
        if (vm.week.value != null) {
            onSuccess(null)
        } else {
            vm.onRefresh()
        }

        vm.date.value?.let { view.setDate(it) }
        vm.date.observe({ lifecycle }) { view.setDate(it) }

        //loads the homework list if it wasn't already done
        if (vmHomework.homework.value != null) {
            onSuccess(null)
        } else {
            vmHomework.onRefresh()
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
        if (!initDate() && !vm.isRefreshing.value!!) {

            Log.i(TAG, "Showing data")

            //updates data
            val week = vm.week.value!!
            val date = vm.date.value!!
            val homework = vmHomework.homework.value

            val day = week.getDay(date)
            if (day == null) {
                view.setError(resources.getString(R.string.error_no_timetable_for_today))
            } else {
                view.updateTimetable(week, day, homework)
            }
        }
    }

    private fun onFail() {
        view.setError(resources.getString(R.string.error_no_timetable_no_internet))
    }
}
