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

package cz.lastaapps.bakalariextension.api.events

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr
import cz.lastaapps.bakalariextension.api.events.data.Event
import cz.lastaapps.bakalariextension.api.events.data.EventList
import cz.lastaapps.bakalariextension.tools.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.ZonedDateTime

class EventsLoader {

    enum class EventType(val url: String, val group: Int) {
        MY("my", Event.GROUP_MY),
        PUBLIC("public", Event.GROUP_PUBIC);

        override fun toString(): String {
            return url
        }
    }

    companion object {
        private val TAG = EventsLoader::class.java.simpleName

        /**Tries to load homework
         * At first from storage
         * if it can be outdated or isn't downloaded yet,
         * tries to download from server
         * if fails, return null*/
        suspend fun load(
            from: ZonedDateTime = TimeTools.firstSeptember,
            forceReload: Boolean = false
        ): EventList? {
            return withContext(Dispatchers.Default) {

                var toReturn: EventList? = null

                if (forceReload || shouldReload()) {
                    toReturn = loadFromServer(from)
                } else {
                    if (!shouldReload())
                        toReturn = loadFromStorage()

                    if (toReturn == null) {
                        toReturn = loadFromServer(from)
                    }
                }

                return@withContext toReturn
            }
        }

        /**Tries load homework from server and save him to local storage
         * @return downloaded AllSubjects object or null, if download failed*/
        suspend fun loadFromServer(from: ZonedDateTime = TimeTools.firstSeptember): EventList? {

            val pairs = ArrayList<Pair<EventType, EventList>>()

            for (type in EventType.values()) {
                loadFromServer(type.url, from)?.let {
                    pairs.add(Pair(type, it))

                    for (event in it) {
                        event.group = type.group
                    }
                }
            }

            if (pairs.isEmpty())
                return null

            return combineEvents(pairs)
        }

        suspend fun loadFromServer(
            type: String,
            from: ZonedDateTime = TimeTools.firstSeptember
        ): EventList? {
            return withContext(Dispatchers.Default) {
                try {
                    val date = TimeTools.format(from, TimeTools.DATE_FORMAT)

                    Log.i(TAG, "Loading from server $type from $date")

                    //downloads homework
                    val dataMap = mapOf(Pair("from", date))
                    val json =
                        withContext(Dispatchers.IO) {
                            ConnMgr.serverGet(
                                "events/${type}",
                                dataMap
                            )
                        }
                            ?: return@withContext null

                    //parses json
                    val data = EventsParser.parseJson(json)

                    //saves json
                    save(type, data, json)

                    return@withContext data

                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext null
                }
            }
        }

        /**Updates homework with new data
         * useful when only last week was loaded, so old data are updated, not replaced*/
        private fun save(type: String, new: EventList, newJson: JSONObject) {

            //EventsStorage.save(newJson, type)
            //return

            val saved = EventsStorage.load(type)
            if (saved == null) {
                //no new data
                EventsStorage.save(newJson, type)
            } else {

                //false when no new data downloaded
                if (new.isNotEmpty()) {
                    val old = EventsParser.parseJson(saved)

                    val combined = HashSet<Event>().apply {
                        //ads old data to new, shorter package
                        addAll(new)
                        addAll(old)
                    }

                    EventsStorage.save(EventsParser.encodeJson(combined.toList()), type)
                } else {

                    //to update lastUpdated variable
                    EventsStorage.save(saved, type)
                }
            }
        }


        /**Loads homework from local storage
         * @return AllSubjects or null, if there is no week of the date save yet*/
        suspend fun loadFromStorage(): EventList? {

            val pairs = ArrayList<Pair<EventType, EventList>>()

            for (type in EventType.values()) {
                loadFromStorage(type.url)?.let {
                    pairs.add(Pair(type, it))

                    for (event in it) {
                        event.group = type.group
                    }
                }
            }

            if (pairs.isEmpty())
                return null

            return combineEvents(pairs)
        }

        suspend fun loadFromStorage(type: String): EventList? {
            return withContext(Dispatchers.Default) {

                Log.i(TAG, "Loading from storage $type")

                return@withContext try {

                    //load json from storage
                    val json = withContext(Dispatchers.IO) { EventsStorage.load(type) }
                        ?: return@withContext null

                    EventsParser.parseJson(json)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        private fun combineEvents(pairs: List<Pair<EventType, EventList>>): EventList {
            val combined = HashSet<Event>()
            val duplicates = HashSet<Event>()

            for (pair in pairs) {
                for (event in pair.second) {
                    if (!combined.add(event)) {
                        duplicates.add(event)
                        event.group = 0
                    }
                }
            }

            for (pair in pairs) {
                for (event in duplicates) {
                    if (pair.second.contains(event))
                        event.group += pair.first.group
                }
            }

            return EventList(combined.toList().sorted())
        }

        /**@return if data is "old" enough to be reloaded*/
        fun shouldReload(): Boolean {
            for (type in EventType.values()) {
                if (shouldReload(type))
                    return true
            }

            return false
        }

        fun shouldReload(type: EventType): Boolean {
            val lastUpdated = EventsStorage.lastUpdated(type.url)
            if (lastUpdated != null) {
                if (lastUpdated.isAfter(TimeTools.now.minusDays(1))) {
                    return false
                }
            }

            return true
        }
    }
}