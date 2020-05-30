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
import cz.lastaapps.bakalariextension.api.timetable.data.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.ZonedDateTime

class TimetableParser {
    companion object {
        private val TAG = TimetableParser::class.java.simpleName

        /**Parses timetable from json, scheme on https://github.com/bakalari-api/bakalari-api-v3
         * @param loadedForDate which date was put into TTStorage or to API request*/
        fun parseJson(loadedForDate: ZonedDateTime, root: JSONObject): Week {

            Log.i(TAG, "Parsing timetable json")

            return Week(
                parseHours(root.getJSONArray("Hours")),
                parseDays(root.getJSONArray("Days")),
                parseClasses(root.getJSONArray("Classes")),
                parseGroups(root.getJSONArray("Groups")),
                parseSubjects(root.getJSONArray("Subjects")),
                parseTeachers(root.getJSONArray("Teachers")),
                parseRooms(root.getJSONArray("Rooms")),
                parseCycles(root.getJSONArray("Cycles")),
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

        private fun parseDays(jsonArray: JSONArray): ArrayList<Day> {
            val list = ArrayList<Day>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Day(
                    json.getInt("DayOfWeek"),
                    json.getString("Date"),
                    json.getString("DayDescription"),
                    json.getString("DayType"),
                    parseLessons(json.getJSONArray("Atoms"))
                )

                list.add(item)
            }

            return list
        }

        private fun parseLessons(jsonArray: JSONArray): DataIdList<Lesson> {
            val list = DataIdList<Lesson>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val groupIds = ArrayList<String>()
                val cycleIds = ArrayList<String>()
                val homeworkIds = ArrayList<String>()

                val loadUp = {
                    key: String, l: ArrayList<String> ->
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

                val item = Lesson(
                    json.getInt("HourId"),
                    groupIds,
                    safeJson(json, "SubjectId"),
                    safeJson(json, "TeacherId"),
                    safeJson(json, "RoomId"),
                    cycleIds,
                    parseChange(change),
                    homeworkIds,
                    safeJson(json, "Theme")
                )

                list.add(item)
            }

            return list
        }

        private fun parseChange(json: JSONObject?): Change? {
            if (json == null) return null

            return Change(
                safeJson(json, "ChangeSubject"),
                json.getString("Day"),
                json.getString("Hours"),
                json.getString("ChangeType"),
                safeJson(json, "description"),
                json.getString("Time"),
                json.getString("TypeAbbrev"),
                json.getString("TypeName")
            )
        }

        private fun parseClasses(jsonArray: JSONArray): DataIdList<Class> {
            val list = DataIdList<Class>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Class(
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

        private fun parseSubjects(jsonArray: JSONArray): DataIdList<Subject> {
            val list = DataIdList<Subject>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Subject(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseTeachers(jsonArray: JSONArray): DataIdList<Teacher> {
            val list = DataIdList<Teacher>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Teacher(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseRooms(jsonArray: JSONArray): DataIdList<Room> {
            val list = DataIdList<Room>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Room(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            return list
        }

        private fun parseCycles(jsonArray: JSONArray): DataIdList<Cycle> {
            val list = DataIdList<Cycle>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Cycle(
                    json.getString("Id"),
                    json.getString("Abbrev"),
                    json.getString("Name")
                )

                list.add(item)
            }

            list.sort()
            return list
        }

        /**try to get String for the key given, protection again JSONException
         * and replacing null object with ""*/
        private fun safeJson(json: JSONObject, key: String): String {
            return try {
                return json.getString(key) ?: ""
            } catch (e: JSONException) {
                ""
            }
        }
    }
}