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

package cz.lastaapps.bakalariextension.ui.events

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.events.EventsRepository
import cz.lastaapps.bakalariextension.databinding.TemplateOverviewBinding
import cz.lastaapps.bakalariextension.tools.MySettings
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toCzechDate
import cz.lastaapps.bakalariextension.tools.validDate
import cz.lastaapps.bakalariextension.ui.timetable.TimetableMainViewModel
import cz.lastaapps.bakalariextension.ui.timetable.TimetableViewModel
import java.time.ZonedDateTime

/**Shows upcoming event for today or tomorrow based on settings*/
class EventsUpcomingFragment : Fragment() {

    companion object {
        private val TAG = EventsUpcomingFragment::class.simpleName
    }

    private lateinit var binding: TemplateOverviewBinding
    private val viewModel: EventsViewModel by activityViewModels()
    private val mainTimetableViewModel: TimetableMainViewModel by activityViewModels()
    private lateinit var currentTimetableViewModel: TimetableViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //inflates views
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.template_overview,
                container,
                false
            )
        binding.setLifecycleOwner { lifecycle }
        binding.viewModel = viewModel
        binding.drawable = R.drawable.module_events
        //TODO contentDescription
        binding.contentDescription = ""

        //navigates to corresponding fragment
        binding.contentLayout.setOnClickListener {
            it.findNavController().navigate(R.id.nav_events)
        }

        //starts loading if data aren't loaded yet
        viewModel.runOrRefresh(lifecycle) { dataChanged() }

        currentTimetableViewModel =
            mainTimetableViewModel.getTimetableViewModel(TimeTools.today.toCzechDate())
        currentTimetableViewModel.runOrRefresh(lifecycle) { dataChanged() }

        return binding.root
    }

    /**sets actual content of the fragment*/
    private fun dataChanged() {

        val events = viewModel.data.value ?: return
        val week = currentTimetableViewModel.data.value

        Log.i(TAG, "Data changed, updating")

        val sett = MySettings(requireContext())
        val date = validDate(week, sett.EVENTS_SHOW_FOR_DAY, R.array.sett_events_show_for_day)
            ?: ZonedDateTime.now()

        var myEvents = 0
        var publicEvents = 0

        for (event in events) {
            if (event.eventStart.toLocalDate() <= date.toLocalDate()
                && date.toLocalDate() <= event.eventEnd.toLocalDate()
            ) {
                if (event.group and EventsRepository.EventType.MY.group > 0)
                    myEvents++
                if (event.eventStart < date && date < event.eventEnd) {
                    publicEvents++
                }
            }
        }

        val text = if (myEvents > 0 || publicEvents > 0) {
            val myEventsText =
                resources.getQuantityString(R.plurals.events_upcoming_any, myEvents)
            val publicEventsText =
                resources.getQuantityString(R.plurals.events_upcoming_any, publicEvents)

            //x your events and y public events for today
            val template = getString(R.string.events_upcoming_placeholder)

            String.format(
                template,
                myEvents,
                myEventsText,
                publicEvents,
                publicEventsText,
                getDayText(date)
            )
        } else {
            getString(R.string.events_upcoming_no)
        }

        binding.text.text = text
    }

    private fun getDayText(date: ZonedDateTime): String {
        val today = TimeTools.today

        val duration = TimeTools.betweenMidnights(today, date)
        return when (duration.toDays().toInt()) {
            0 -> getString(R.string.events_upcoming_today)
            1 -> getString(R.string.events_upcoming_tomorrow)
            else -> TimeTools.format(date, "EEEE") //day name
        }
    }
}