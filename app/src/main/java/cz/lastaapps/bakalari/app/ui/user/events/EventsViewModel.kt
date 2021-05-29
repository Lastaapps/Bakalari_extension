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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.api.entity.events.EventList
import cz.lastaapps.bakalari.api.repo.events.EventsRepository
import cz.lastaapps.bakalari.api.repo.events.eventsRepository
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.tools.ui.RefreshableListViewModel

class EventsViewModel : RefreshableListViewModel<EventList, EventsRepository>(
    TAG, CurrentUser.requireDatabase().eventsRepository
) {

    companion object {
        private val TAG = EventsViewModel::class.java.simpleName
    }

    override val data by lazy { repo.getEvents().asLiveData() }

    suspend fun getEvent(id: String) = repo.getEvent(id)

    init {
        addEmptyObserver()
    }

    /** Current main filter id - all, my, public*/
    val filterId = MutableLiveData(R.id.filter_all)

    /** If advanced filter is shown*/
    val advancedFilterVisible = MutableLiveData(false)

    /** The place to observe for advanced menu updates*/
    val advancedUpdatedToObserve = MutableLiveData<Any>()

    /** Contains LiveData containing states for all the advanced types filters*/
    private val advancedFiltersMap = HashMap<SimpleData, MutableLiveData<Boolean>>()

    /**Text to filter events*/
    val filterText = MutableLiveData("")

    /** If select all is checked right now*/
    val selectAllChecked = MutableLiveData(true).also {
        //called when state got updated
        it.observeForever { checked ->
            if (checked) {

                //if we got to true state, all the other choices should be set to true
                for (value in advancedFiltersMap.values) {
                    if (value.value != true) {
                        value.value = true
                    }
                }
            } else {
                //if we got to false state, if all the others choices are true, then we need to
                //trn them false. If there is mix of true and false, we do nothing

                var trueOnly = true

                //detects if all the views are true
                for (data in advancedFiltersMap.values) {
                    if (data.value == false) {
                        trueOnly = false
                        break
                    }
                }

                //changes all the views to false if they are true
                if (trueOnly)
                    for (value in advancedFiltersMap.values) {
                        if (value.value != false) {
                            value.value = false
                        }
                    }
            }
        }
    }

    /** Called when oder choice is changed, updates select all*/
    private val advancedFilterUpdated: ((Boolean) -> Unit) = { _ ->

        //notifies filter
        advancedUpdatedToObserve.value = Any()

        //determinate the state of the select all
        var trueOnly = true

        for (data in advancedFiltersMap.values) {
            if (data.value == false) {
                trueOnly = false
                break
            }
        }

        //sets select all to valid state
        if (selectAllChecked.value != trueOnly)
            selectAllChecked.value = trueOnly
    }

    /**@return live data containing the state for the given state filter*/
    fun getAdvanceLiveData(type: SimpleData): MutableLiveData<Boolean> {
        advancedFiltersMap.apply {
            return if (containsKey(type)) {
                get(type)!!
            } else {
                MutableLiveData(true).also {
                    //sets default values and integrates into system
                    put(type, it)
                    it.observeForever(advancedFilterUpdated)
                }
            }
        }

    }

    fun getAdvanceSelectedFilters(): List<SimpleData> {
        val list = ArrayList<SimpleData>()

        for (key in advancedFiltersMap.keys) {
            if (advancedFiltersMap[key]!!.value == true)
                list.add(key)
        }

        return list
    }

    override fun failedText(context: Context): String {
        return context.getString(R.string.events_failed_to_load)
    }

    override fun emptyText(context: Context): String {
        return context.getString(R.string.events_no_events)
    }

}