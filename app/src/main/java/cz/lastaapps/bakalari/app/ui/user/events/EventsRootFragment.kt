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

package cz.lastaapps.bakalari.app.ui.user.events

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import cz.lastaapps.bakalari.api.core.SimpleData
import cz.lastaapps.bakalari.api.core.events.holders.Event
import cz.lastaapps.bakalari.api.core.events.holders.EventList
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentEventsBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCzechDate
import cz.lastaapps.bakalari.tools.ui.EmptyAdapter
import cz.lastaapps.bakalari.tools.ui.lastUpdated
import kotlin.math.max

/**The main fragment for all the events*/
class EventsRootFragment : Fragment() {

    companion object {
        private val TAG = EventsRootFragment::class.java.simpleName
    }

    private val viewModel: EventsViewModel by accountsViewModels()
    private lateinit var binding: FragmentEventsBinding

    /**clears search text on back press*/
    private val clearText: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            clearText()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating view")

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_events, container, false)
        binding.viewModel = viewModel
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
        viewModel.onDataUpdate(lifecycle) { lastUpdatedText() }

        //observes for the data change
        viewModel.runOrRefresh(lifecycle) { dataChanged(it) }

        viewModel.filterText.observe({ lifecycle }) { showData() }

        viewModel.filterId.observe({ lifecycle }) { showData() }

        viewModel.advancedUpdatedToObserve.observe({ lifecycle }) { showData() }

        //clears filters on bck press
        requireActivity().onBackPressedDispatcher.addCallback({ lifecycle }, clearText)
        val changeListener = { _: Any ->
            clearText.isEnabled = viewModel.filterText.value != ""
        }
        viewModel.filterText.observe({ lifecycle }, changeListener)

        return binding.root
    }

    /**clears search text on back press*/
    fun clearText() {
        viewModel.filterText.value = ""
    }

    /**when data changed - updates all the components*/
    private fun dataChanged(events: EventList) {

        //advance filter setup
        val types = HashSet<SimpleData>()

        for (event in events)
            types.add(event.type)

        //creates live data
        for (type in types)
            viewModel.getAdvanceLiveData(type)

        (binding.advancedFilter.adapter as EventAdvanceFilterAdapter).update(
            types.toList().sorted()
        )

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

        for (index in 0 until filtered.size) {
            val event = filtered[index]

            if (event.eventStart.toCzechDate() <= now.toCzechDate()) {

                scrollTo = index
                break
            }
        }

        scrollTo = max(scrollTo - 3, 0) //scrolls to 3rd item before the most recent one

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

        val textFiltered = Event.filterByText(filtered, viewModel.filterText.value ?: "")

        return cz.lastaapps.bakalari.api.core.events.holders.EventList(textFiltered)
    }

    private fun lastUpdatedText() {
        //last updated text
        val lastUpdatedDate = viewModel.lastUpdated()
        binding.lastUpdated.text =
            if (lastUpdatedDate != null) {
                lastUpdated(requireContext(), lastUpdatedDate)
            } else
                viewModel.failedText(requireContext())
    }
}