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

package cz.lastaapps.bakalariextension.widgets.smalltimetable

import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.api.database.APIBase
import cz.lastaapps.bakalariextension.api.timetable.TimetableRepository
import cz.lastaapps.bakalariextension.api.timetable.data.Week
import cz.lastaapps.bakalariextension.tools.TimeTools.Companion.toMonday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WidgetData private constructor(private val userId: String) {

    private val database = APIBase.getDatabase(userId)
    private val mainRepo = database.timetableRepository
    private var currentRepo: TimetableRepository? = null
    private var subscription: ReceiveChannel<Week?>? = null

    private var week: Week? = null
    fun getWeek() = week

    private var hasData = false
    fun hasData() = hasData

    init {
        updateDate()
    }

    fun updateDate() {
        scope.launch(Dispatchers.Default) {

            val now = LocalDate.now()

            if (currentRepo == null || now.toMonday() != week?.monday
            ) {
                subscription?.cancel()

                currentRepo = mainRepo.getRepositoryForDate(now)
                subscription = currentRepo!!.getWeek().broadcastIn(scope).openSubscription()

                week = null
                hasData = false
                observeForChange()
            }
        }
    }

    private fun observeForChange() {
        scope.launch(Dispatchers.Default) {
            subscription?.consumeEach {
                week = it
                hasData = true
                notifyObservers()
            }
        }
    }

    private val observers = HashSet<Int>()

    fun registerObserver(id: Int) = observers.add(id)
    fun removeObserver(id: Int) {
        observers.remove(id)
        if (observers.isEmpty()) {
            subscription?.cancel()
            subscription = null
            currentRepo = null
            week = null
        }
    }

    fun registerObservers(ids: List<Int>) = ids.forEach { registerObserver(it) }
    fun removeObservers(ids: List<Int>) = ids.forEach { registerObserver(it) }

    fun getObservers() = observers.toMutableSet()

    private fun notifyObservers() {
        SmallTimetableWidget.updateIds(App.context, observers.toIntArray())
    }

    companion object {

        private val scope = CoroutineScope(Dispatchers.Default)
        private val data = HashMap<String, WidgetData>()

        fun getAll() = data.toMutableMap()

        fun getWidgetData(userId: String): WidgetData {
            return if (data.containsKey(userId))
                data[userId]!!
            else {
                val v = WidgetData(userId)
                data[userId] = v
                v
            }
        }
    }
}