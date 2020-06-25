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
import cz.lastaapps.bakalariextension.api.marks.data.MarksRoot
import cz.lastaapps.bakalariextension.api.marks.data.Subject
import cz.lastaapps.bakalariextension.api.marks.data.SubjectMarks
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class MarksParser {
    companion object {
        private val TAG = MarksParser::class.java.simpleName

        /**Parses marks from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
        fun parseJson(root: JSONObject): MarksRoot {

            Log.i(TAG, "Parsing marks json")

            //parses whole json
            return MarksRoot(
                ArrayList(
                    parseAllSubjects(root.getJSONArray("Subjects")).sorted()
                )
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
                    safeJson(json, "MarkDate"),
                    safeJson(json, "EditDate"),
                    safeJson(json, "Caption"),
                    safeJson(json, "Theme"),
                    safeJson(json, "MarkText"),
                    safeJson(json, "TeacherId"),
                    safeJson(json, "Type"),
                    safeJson(json, "TypeNote"),
                    safeInt(json, "Weight", 4),
                    safeJson(json, "SubjectId"),
                    json.getBoolean("IsNew"),
                    json.getBoolean("IsPoints"),
                    safeJson(json, "CalculatedMarkText"),
                    safeJson(json, "ClassRankText"), //seen only as null
                    safeJson(json, "Id"),
                    safeJson(json, "PointsText"),
                    safeInt(json, "MaxPoints", 100)
                )

                list.add(item)
            }

            return DataIdList(list.sorted())
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

        /**try to get Int for the key given, protection again JSONException
         * and replacing null object with default value parameter*/
        private fun safeInt(json: JSONObject, key: String, default: Int = 0): Int {
            return try {
                if (!json.isNull(key))
                    return json.getInt(key)
                else
                    default
            } catch (e: JSONException) {
                default
            }
        }
    }
}