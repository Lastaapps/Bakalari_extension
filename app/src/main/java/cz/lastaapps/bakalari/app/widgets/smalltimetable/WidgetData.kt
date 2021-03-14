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

package cz.lastaapps.bakalari.app.widgets.smalltimetable

import android.content.Context
import cz.lastaapps.bakalari.app.api.database.APIBase
import cz.lastaapps.bakalari.app.api.timetable.TimetableRepository
import cz.lastaapps.bakalari.app.api.timetable.data.Week
import cz.lastaapps.bakalari.platform.App
import cz.lastaapps.bakalari.tools.TimeTools.toMonday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class WidgetData @Throws(IllegalArgumentException::class) private constructor(
    context: Context,
    userId: String
) {

    private val database = APIBase.getDatabaseBlocking(context, UUID.fromString(userId))!!
    private val mainRepo = database.timetableRepository
    private var currentRepo: TimetableRepository? = null
    private var shared: SharedFlow<Week?>? = null

    private var collectingJob: Job? = null

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
                collectingJob?.cancel()
                collectingJob = null

                currentRepo = mainRepo.getRepositoryForDate(now)

                shared = currentRepo!!.getWeek()
                    .shareIn(this, SharingStarted.WhileSubscribed(), 1)

                week = null
                hasData = false
                observeForChange()
            }
        }
    }

    private fun observeForChange() {
        collectingJob = scope.launch(Dispatchers.Default) {
            shared?.collect {
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

            collectingJob?.cancel()
            collectingJob = null
            shared = null

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

        fun getAll() = synchronized(this) { data.toMutableMap() }

        @Throws(IllegalArgumentException::class)
        fun getWidgetData(context: Context, userId: String): WidgetData = synchronized(this) {
            return if (data.containsKey(userId))
                data[userId]!!
            else {
                val v = WidgetData(context, userId)
                data[userId] = v
                v
            }
        }

        fun removeObservers(ids: List<Int>) = synchronized(this) {
            for (item in data.values) {
                item.removeObservers(ids)
            }
        }
    }
}