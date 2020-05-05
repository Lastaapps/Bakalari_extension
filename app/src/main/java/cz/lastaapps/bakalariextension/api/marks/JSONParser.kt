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

package cz.lastaapps.bakalariextension.api.marks

import android.util.Log
import cz.lastaapps.bakalariextension.api.DataIdList
import cz.lastaapps.bakalariextension.api.marks.data.Mark
import cz.lastaapps.bakalariextension.api.marks.data.MarksAllSubjects
import cz.lastaapps.bakalariextension.api.marks.data.Subject
import cz.lastaapps.bakalariextension.api.marks.data.SubjectMarks
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class JSONParser {
    companion object {
        private val TAG = JSONParser::class.java.simpleName

        /**Parses marks from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
        fun parseJson(root: JSONObject): MarksAllSubjects {

            Log.i(TAG, "Parsing marks json")

            //parses whole json
            return MarksAllSubjects(
                parseAllSubjects(root.getJSONArray("Subjects"))
            )
        }

        /**parse array in /Subjects */
        private fun parseAllSubjects(jsonArray: JSONArray): ArrayList<SubjectMarks> {
            val list = ArrayList<SubjectMarks>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = SubjectMarks(
                    parseMarks(json.getJSONArray("Marks")),
                    parseSubject(json.getJSONObject("Subject")),
                    json.getString("AverageText"),
                    json.getString("TemporaryMark"),
                    json.getString("SubjectNote"),
                    json.getString("TemporaryMarkNote"),
                    json.getBoolean("PointsOnly"),
                    json.getBoolean("MarkPredictionEnabled")
                )

                list.add(item)
            }

            return list
        }

        /**parse array in /Subjects/Marks */
        private fun parseMarks(jsonArray: JSONArray): DataIdList<Mark> {
            val list = DataIdList<Mark>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Mark(
                    json.getString("MarkDate"),
                    json.getString("EditDate"),
                    json.getString("Caption"),
                    json.getString("Theme"),
                    json.getString("MarkText"),
                    json.getString("TeacherId"),
                    json.getString("Type"),
                    json.getString("TypeNote"),
                    json.getInt("Weight"),
                    json.getString("SubjectId"),
                    json.getBoolean("IsNew"),
                    json.getBoolean("IsPoints"),
                    json.getString("CalculatedMarkText"),
                    saveJson(json, "ClassRankText"), //seen only as null
                    json.getString("Id"),
                    json.getString("PointsText"),
                    json.getInt("MaxPoints")
                )

                list.add(item)
            }

            return list
        }

        /**parse array in /Subjects/Subject */
        private fun parseSubject(json: JSONObject): Subject {

            return Subject(
                json.getString("Id"),
                json.getString("Abbrev").trim(),
                json.getString("Name")
            )
        }

        /**try to get String for the key given, protection again JSONException
         * and replacing null object with ""*/
        private fun saveJson(json: JSONObject, key: String): String {
            return try {
                return json.getString(key) ?: ""
            } catch (e: JSONException) {
                ""
            }
        }
    }
}