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

package cz.lastaapps.bakalariextension.api.timetable

import android.util.Log
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.SimpleData
import cz.lastaapps.bakalariextension.api.timetable.data.*
import cz.lastaapps.bakalariextension.tools.TimeTools
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

class TimetableParser {
    companion object {
        private val TAG = TimetableParser::class.java.simpleName

        /**Parses timetable from json, scheme on https://github.com/bakalari-api/bakalari-api-v3
         * @param loadedForDate which date was put into TTStorage or to API request*/
        fun parseJson(loadedForDate: LocalDate, root: JSONObject): Week {

            Log.i(TAG, "Parsing timetable json")

            //permanent table has custom dates used
            val isPermanent = loadedForDate == TimeTools.PERMANENT

            return Week(
                parseHours(root.getJSONArray("Hours")),
                parseDays(isPermanent, root.getJSONArray("Days")),
                parseSimple(root.getJSONArray("Classes")),
                parseGroups(root.getJSONArray("Groups")),
                parseSimple(root.getJSONArray("Subjects")),
                parseSimple(root.getJSONArray("Teachers")),
                parseSimple(root.getJSONArray("Rooms")),
                parseSimple(root.getJSONArray("Cycles")),
                loadedForDate
            )
        }

        private fun parseHours(jsonArray: JSONArray): DataIdList<Hour> {
            val list = DataIdList<Hour>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Hour(
                    json.getInt("Id"),
                    json.getString("Caption"),
                    json.getString("BeginTime"),
                    json.getString("EndTime")
                )

                list.add(item)
            }

            list.sort()
            return list
        }

        private fun parseDays(isPermanent: Boolean, jsonArray: JSONArray): ArrayList<Day> {
            val list = ArrayList<Day>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val dayOfWeek = json.getInt("DayOfWeek")
                val date =
                    if (!isPermanent)
                        parseDate(json.getString("Date"))
                    else
                        TimeTools.PERMANENT.plusDays(dayOfWeek - 1L)

                val description = json.getString("DayDescription")

                val item = Day(
                    dayOfWeek,
                    date,
                    description,
                    updateDayDescription(description),
                    json.getString("DayType"),
                    parseLessons(date, json.getJSONArray("Atoms"))
                )

                list.add(item)
            }

            return list
        }

        /**Easter egg - add emojis to celebration*/
        private fun updateDayDescription(string: String): String {
            if (string == "") return string

            //TODO find them all!
            if (string == "Den české státnosti") return string + "\uD83C\uDDE8\uD83C\uDDFF"
            if (string == "Den vzniku samostatného Československého státu") return string + "\uD83C\uDDF8\uD83C\uDDF0"
            if (string == "Den boje za svobodu a demokracii") return "$string\uD83D\uDDF3️"
            if (string == "Štědrý den") return string + "\uD83C\uDF81"
            if (string == "1. svátek vánoční") return string + "\uD83C\uDF84"
            if (string == "2. svátek vánoční") return "$string❄️"
            if (string == "Nový rok") return string + "\uD83C\uDF86"
            if (string == "jarní prázdniny") return "$string☀️"
            if (string == "Cyril a Metoděj") return "$string☦️"
            if (string == "Mistr Jan Hus") return string + "\uD83D\uDD25"

            return string
        }

        private fun parseLessons(date: LocalDate, jsonArray: JSONArray): DataIdList<Lesson> {
            val list = DataIdList<Lesson>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val groupIds = ArrayList<String>()
                val cycleIds = ArrayList<String>()
                val homeworkIds = ArrayList<String>()

                val loadUp = { key: String, l: ArrayList<String> ->
                    val array = json.getJSONArray(key)

                    for (j in 0 until array.length()) {
                        l.add(array.getString(j))
                    }
                }

                loadUp("GroupIds", groupIds)
                loadUp("CycleIds", cycleIds)
                loadUp("HomeworkIds", homeworkIds)

                val change: JSONObject? = if (
                    !json.isNull("Change")) {
                    json.getJSONObject("Change")
                } else {
                    null
                }

                val hourId = json.getInt("HourId")

                val item = Lesson(
                    date,
                    hourId,
                    groupIds,
                    safeJson(json, "SubjectId"),
                    safeJson(json, "TeacherId"),
                    safeJson(json, "RoomId"),
                    cycleIds,
                    parseChange(hourId, change),
                    homeworkIds,
                    safeJson(json, "Theme")
                )

                list.add(item)
            }

            return list
        }

        private fun parseChange(hourId: Int, json: JSONObject?): Change? {
            if (json == null) return null

            return Change(
                safeJson(json, "ChangeSubject"),
                parseDate(safeJson(json, "Day")),
                hourId,
                safeJson(json, "Hours"),
                safeJson(json, "ChangeType"),
                safeJson(json, "Description"),
                safeJson(json, "Time"),
                safeJson(json, "TypeAbbrev"),
                safeJson(json, "TypeName")
            )
        }

        private fun parseSimple(jsonArray: JSONArray): DataIdList<SimpleData> {
            val list = DataIdList<SimpleData>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = SimpleData(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseGroups(jsonArray: JSONArray): DataIdList<Group> {
            val list = DataIdList<Group>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Group(
                    json.getString("ClassId"),
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseDate(date: String): LocalDate =
            TimeTools.parse(date, TimeTools.COMPLETE_FORMAT, TimeTools.CET).toLocalDate()

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
}