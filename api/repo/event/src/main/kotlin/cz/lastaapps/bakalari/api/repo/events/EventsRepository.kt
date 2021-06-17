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

package cz.lastaapps.bakalari.api.repo.events

import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.api.database.JSONStorageRepository
import cz.lastaapps.bakalari.api.entity.core.APIBaseKeys
import cz.lastaapps.bakalari.api.entity.events.EventList
import cz.lastaapps.bakalari.api.repo.core.RefreshingRepo
import cz.lastaapps.bakalari.tools.TimeTools.toMidnight
import cz.lastaapps.bakalari.tools.runTrace
import cz.lastaapps.bakalari.tools.sortList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.ZonedDateTime

private typealias JSONList = List<Pair<EventsRepository.EventType, JSONObject>>

class EventsRepository(database: APIBase) : RefreshingRepo<EventList, JSONList>(TAG, database) {

    companion object {
        private val TAG get() = EventsRepository::class.java.simpleName
    }

    enum class EventType(val url: String, val group: Int) {
        MY("my", cz.lastaapps.bakalari.api.entity.events.Event.GROUP_MY),
        PUBLIC("public", cz.lastaapps.bakalari.api.entity.events.Event.GROUP_PUBIC);

        override fun toString(): String {
            return url
        }
    }

    private val dao = database.eventsDao()

    fun getEvents() = dao.getEvents().distinctUntilChanged()
        .map { list -> EventList(list.map { it.toEvent() }) }
        .onDataUpdated { it.sortList() }

    suspend fun getEvent(id: String) = dao.getEvent(id)

    override fun lastUpdatedTables(): List<String> = listOf(
        APIBaseKeys.EVENTS,
        APIBaseKeys.EVENTS_TIMES,
        APIBaseKeys.EVENTS_CLASSES_RELATIONS,
        APIBaseKeys.EVENTS_TEACHES,
        APIBaseKeys.EVENTS_ROOMS,
        APIBaseKeys.EVENTS_STUDENTS
    )

    override fun shouldReloadDelay(date: ZonedDateTime): ZonedDateTime =
        date.toMidnight().plusDays(1)

    override suspend fun loadFromServer(): JSONList? {
        val pairs = ArrayList<Pair<EventType, JSONObject>>()

        for (type in EventType.values()) {

            //val args = mapOf(Pair("from", TimeTools.format(from, TimeTools.DATE_FORMAT)))
            val loaded = loadFromServer("events/${type.url}" /*args*/) ?: return null

            //val loaded = loadFromAssets("events_${type.url}.json") ?: return null

            pairs.add(Pair(type, loaded))
        }

        return pairs
    }

    /** Saves JSON into json storage table using JSONStorageRepo*/
    override suspend fun saveToJsonStorage(
        repo: JSONStorageRepository,
        json: List<Pair<EventType, JSONObject>>
    ) {
        for (pair in json) {
            repo.saveEvents(pair.first.url, pair.second)
        }
    }

    override suspend fun parseData(json: JSONList): EventList = runTrace(TAG, "Combining") {
        combineEvents(json.map {
            Pair(
                it.first,
                cz.lastaapps.bakalari.api.entity.events.EventsParser.parseJson(it.second)
            )
        })
    }

    private fun combineEvents(pairs: List<Pair<EventType, EventList>>): EventList {

        val combinedIds = HashSet<String>()

        //filters ids
        for (pair in pairs) {
            combinedIds.addAll(pair.second.map { it.id })
        }

        //converts ids back to events
        val combined = EventList()
        for (id in combinedIds) {
            for (pair in pairs) {
                val item = pair.second.getById(id) ?: continue
                combined.add(item)

                break
            }
        }

        //gives events corresponding group id
        for (pair in pairs) {
            for (event in combined) {
                if (pair.second.contains(event))
                    event.group += pair.first.group
            }
        }

        return combined
    }

    /** Inserts data to database
     * @return list of updated tables*/
    override suspend fun insertIntoDatabase(data: EventList): List<String> {
        dao.replaceEvents(data.map {
            cz.lastaapps.bakalari.api.entity.events.EventHolderWithLists.fromEvent(
                it
            )
        })

        return lastUpdatedTables()
    }

    override suspend fun deleteAll() {
        super.deleteAll()
        dao.deleteAll()
    }
}