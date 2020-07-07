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
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.events.data.Event
import cz.lastaapps.bakalariextension.api.events.data.EventList
import cz.lastaapps.bakalariextension.api.events.data.EventTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class EventsParser {
    companion object {
        private val TAG = EventsParser::class.java.simpleName

        /**Parses marks from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
        fun parseJson(root: JSONObject): EventList {

            Log.i(TAG, "Parsing json")

            //parses whole json
            return parseEvents(root.getJSONArray("Events"))
        }

        /**parse array in /Events */
        private fun parseEvents(jsonArray: JSONArray): EventList {
            val list = EventList()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Event(
                    safeJson(json, "Id"),
                    0,
                    safeJson(json, "Title"),
                    safeJson(json, "Description"),
                    parseTimes(json.getJSONArray("Times")),
                    parseSimple(json.getJSONObject("EventType")),
                    parseSimpleArray(json.getJSONArray("Classes")),
                    json.getJSONArray("ClassSets"),
                    parseSimpleArray(json.getJSONArray("Teachers")),
                    json.getJSONArray("TeacherSets"),
                    parseSimpleArray(json.getJSONArray("Rooms")),
                    json.getJSONArray("RoomSets"),
                    parseSimpleArray(json.getJSONArray("Students")),
                    safeJson(json, "Note"),
                    json.getString("DateChanged")
                )

                list.add(item)
            }

            list.sort()

            return list
        }

        /**parses event start and ent times*/
        private fun parseTimes(array: JSONArray): ArrayList<EventTime> {
            val list = ArrayList<EventTime>()

            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)

                list.add(
                    EventTime(
                        json.getBoolean("WholeDay"),
                        json.getString("StartTime"),
                        json.getString("EndTime")
                    )
                )
            }

            return list
        }

        /**parses array of objects*/
        private fun parseSimpleArray(array: JSONArray): DataIdList<SimpleData> {
            val list = DataIdList<SimpleData>()

            for (i in 0 until array.length()) {
                list.add(parseSimple(array.getJSONObject(i)))
            }

            return list
        }

        /**parse data in /Homework/(Class, Group, Subject, Teacher) */
        private fun parseSimple(json: JSONObject): SimpleData {

            return SimpleData(
                json.getString("Id"),
                json.getString("Abbrev").trim(),
                json.getString("Name")
            )
        }

        /**try to get String for the key given, protection again JSONException
         * and replacing null object with ""*/
        private fun safeJson(json: JSONObject, key: String): String {
            return try {
                if (!json.isNull(key))
                    return json.getString(key)
                else
                    ""
            } catch (e: JSONException) {
                ""
            }
        }


        /**Encodes data list back to JSON*/
        fun encodeJson(list: List<Event>): JSONObject {

            return JSONObject().apply {
                put("Events", encodeEvents(EventList(list)))
            }
        }

        /**encodes array in /Events */
        private fun encodeEvents(list: EventList): JSONArray {
            val array = JSONArray()

            for (event in list) {
                array.put(JSONObject().apply {
                    put("Id", event.id)
                    put("Title", event.title)
                    put("Description", event.description)
                    put("Times", encodeTimes(event.times))
                    put("EventType", encodeSimple(event.type))
                    put("Classes", encodeSimpleArray(event.classes))
                    put("ClassSets", event.classSets)
                    put("Teachers", encodeSimpleArray(event.teachers))
                    put("TeacherSets", event.teacherSets)
                    put("Rooms", encodeSimpleArray(event.rooms))
                    put("RoomSets", event.roomSet)
                    put("Students", encodeSimpleArray(event.students))
                    put("Note", event.note)
                    put("DateChanged", event.dateChanged)
                })
            }

            return array
        }

        /**encodes event start and ent times*/
        private fun encodeTimes(list: ArrayList<EventTime>): JSONArray {
            val array = JSONArray()

            for (time in list) {
                array.put(JSONObject().apply {
                    put("WholeDay", time.wholeDay)
                    put("StartTime", time.dateStart)
                    put("EndTime", time.dateEnd)
                })
            }

            return array
        }

        /**encodes array of objects*/
        private fun encodeSimpleArray(list: DataIdList<SimpleData>): JSONArray {
            val array = JSONArray()

            for (data in list) {
                array.put(encodeSimple(data))
            }

            return array
        }

        /**encodes group, class, subject and teacher back to JSON*/
        private fun encodeSimple(data: SimpleData): JSONObject {
            return JSONObject().apply {
                put("Id", data.id)
                put("Abbrev", data.shortcut)
                put("Name", data.name)
            }
        }
    }
}