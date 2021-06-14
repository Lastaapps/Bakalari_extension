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

package cz.lastaapps.bakalari.api.entity.events

import android.util.Log
import cz.lastaapps.bakalari.api.entity.core.DataIdList
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCommon
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.ZonedDateTime

object EventsParser {
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

            val id = safeJson(json, "Id")
            val times = parseTimes(id, json.getJSONArray("Times"))

            val item = Event(
                id,
                0,
                safeJson(json, "Title"),
                safeJson(json, "Description"),
                times,
                Event.getStartTime(times),
                Event.getEndTime(times),
                parseSimple(json.getJSONObject("EventType")),
                parseSimpleArray(json.getJSONArray("Classes")),
                //json.getJSONArray("ClassSets"),
                parseSimpleArray(json.getJSONArray("Teachers")),
                //json.getJSONArray("TeacherSets"),
                parseSimpleArray(json.getJSONArray("Rooms")),
                //json.getJSONArray("RoomSets"),
                parseSimpleArray(json.getJSONArray("Students")),
                safeJson(json, "Note"),
                TimeTools.parse(json.getString("DateChanged"), TimeTools.COMPLETE_FORMAT)
                    .toCommon()
            )

            list.add(item)
        }

        list.sort()

        return list
    }

    /**parses event start and ent times*/
    private fun parseTimes(id: String, array: JSONArray): ArrayList<EventTime> {
        val list = ArrayList<EventTime>()

        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)

            list.add(
                EventTime(
                    id,
                    json.getBoolean("WholeDay"),
                    parseTime(json.getString("StartTime")),
                    parseTime(json.getString("EndTime"))
                )
            )
        }

        return list
    }

    //time formats differ not sure why and when, to try catch is used
    private fun parseTime(time: String): ZonedDateTime {
        return try {
            TimeTools.parse(time, TimeTools.COMPLETE_FORMAT)
        } catch (e: Exception) {
            TimeTools.parse(time, TimeTools.COMPLETE_SHORTER)
        }.toCommon()
    }

    /**parses array of objects*/
    private fun parseSimpleArray(array: JSONArray): DataIdList<SimpleData> {
        val list = DataIdList<SimpleData>()

        for (i in 0 until array.length()) {
            parseSimple(array.getJSONObject(i)).also {
                if (it.id != "")
                    list.add(it)
            }
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
}
