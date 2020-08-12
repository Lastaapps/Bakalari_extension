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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.events.EventsLoader
import cz.lastaapps.bakalariextension.api.events.EventsStorage
import cz.lastaapps.bakalariextension.api.events.data.Event
import cz.lastaapps.bakalariextension.api.events.data.EventList
import cz.lastaapps.bakalariextension.databinding.FragmentEventsBinding
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.tools.lastUpdated
import cz.lastaapps.bakalariextension.ui.EmptyAdapter
import java.time.ZonedDateTime

/**The main fragment for all the events*/
class EventsRootFragment : Fragment() {

    companion object {
        private val TAG = EventsRootFragment::class.java.simpleName
    }

    private val viewModel: EventsViewModel by activityViewModels()
    private lateinit var binding: FragmentEventsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_events, container, false)
        binding.viewmodel = viewModel
        binding.setLifecycleOwner { lifecycle }

        binding.list.adapter = EmptyAdapter(EventsAdapter())

        //shows and hides the advanced options
        binding.advanced.setOnClickListener {
            viewModel.advancedFilterVisible.apply {
                value = value != true
            }
        }

        //advanced options setup
        binding.advancedFilter.apply {
            layoutManager = FlexboxLayoutManager(requireContext(), FlexDirection.ROW, FlexWrap.WRAP)
            adapter = EventAdvanceFilterAdapter(
                listOf(),
                { viewModel.selectAllChecked },
                { viewModel.getAdvanceLiveData(it) })

        }

        //observes for the data change
        viewModel.executeOrRefresh(lifecycle) { dataUpdated() }

        viewModel.filterId.observe({ lifecycle }) {
            showData()
        }

        viewModel.advancedUpdatedToObserve.observe({ lifecycle }) { showData() }

        return binding.root
    }

    /**when data changed - updates all the components*/
    private fun dataUpdated() {
        val types = HashSet<SimpleData>()

        for (event in viewModel.requireData())
            types.add(event.type)

        //creates live data
        for (type in types)
            viewModel.getAdvanceLiveData(type)

        (binding.advancedFilter.adapter as EventAdvanceFilterAdapter).update(
            types.toList().sorted()
        )


        //last updated text
        //there are more events sets, finds the oldest one
        var theOldest: ZonedDateTime? = null
        for (type in EventsLoader.EventType.values()) {
            EventsStorage.lastUpdated(type.url)?.let {
                if (theOldest == null || it > theOldest)
                    theOldest = it
            }
        }

        binding.lastUpdated.text =
            if (theOldest != null) {
                lastUpdated(requireContext(), theOldest!!)
            } else
                viewModel.failedText(requireContext())

        showData()
    }

    /**updates the event list only*/
    private fun showData() {
        val data = viewModel.data.value ?: return

        val filtered = filter(data.toMutableList())
        EmptyAdapter.getAdapter<EventsAdapter>(binding.list).update(filtered)

        //scrolls to today
        var scrollTo = 0
        val now = TimeTools.today
        for (event in filtered) {
            if (event.eventStart.toLocalDate() <= now.toLocalDate()) {
                scrollTo = filtered.indexOf(event)
                break
            }
        }

        (binding.list.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(scrollTo, 0)
    }

    /**filters out all the unwanted events*/
    private fun filter(list: List<Event>): EventList {

        //basic filter
        val filtered =
            when (viewModel.filterId.value) {
                R.id.filter_all -> list.toMutableList()
                R.id.filter_my -> {
                    val filtered = ArrayList<Event>()

                    for (event in list) {
                        if (event.group and Event.GROUP_MY > 0) {
                            filtered.add(event)
                        }
                    }

                    filtered
                }
                R.id.filter_public -> {
                    val filtered = ArrayList<Event>()

                    for (event in list) {
                        if (event.group and Event.GROUP_PUBIC > 0) {
                            filtered.add(event)
                        }
                    }

                    filtered
                }
                else -> list.toMutableList()
            }

        //advance filter
        val selected = viewModel.getAdvanceSelectedFilters()
        for (event in filtered.toMutableList()) {
            if (!selected.contains(event.type))
                filtered.remove(event)
        }

        return EventList(filtered)
    }

}