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

package cz.lastaapps.bakalariextension.api.subjects

import android.util.Log
import cz.lastaapps.bakalariextension.api.subjects.data.Subject
import cz.lastaapps.bakalariextension.api.subjects.data.Teacher
import cz.lastaapps.bakalariextension.api.subjects.data.Theme
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**decodes Theme json*/
class SubjectThemeParser {
    companion object {
        private val TAG = SubjectThemeParser::class.java.simpleName

        /**Parses subjects from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
        fun parseSubjectJson(root: JSONObject): SubjectList {

            Log.i(TAG, "Parsing subjects json")

            //parses whole json
            return SubjectList(
                parseSubjects(root.getJSONArray("Subjects")).sorted()
            )
        }

        /**parse array in /Subjects */
        private fun parseSubjects(jsonArray: JSONArray): SubjectList {
            val list = SubjectList()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Subject(
                    json.getString("SubjectID"),
                    json.getString("SubjectName"),
                    json.getString("SubjectAbbrev"),
                    Teacher(
                        json.getString("TeacherID"),
                        json.getString("TeacherName"),
                        json.getString("TeacherAbbrev"),
                        safeJson(json, "TeacherEmail"),
                        safeJson(json, "TeacherWeb"),
                        safeJson(json, "TeacherSchoolPhone"),
                        safeJson(json, "TeacherHomePhone"),
                        safeJson(json, "TeacherMobilePhone")
                    )
                )

                list.add(item)
            }

            return list
        }

        /**Parses themes json*/
        fun parseThemeJson(root: JSONObject): ThemeList {
            Log.i(TAG, "Parsing theme json")

            //parses whole json
            return ThemeList(
                parseThemes(root.getJSONArray("Themes")).sorted()
            )
        }

        /**parses array containing themes*/
        private fun parseThemes(jsonArray: JSONArray): ThemeList {
            val list = ThemeList()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val item = Theme(
                    json.getString("Date"),
                    json.getString("Theme"),
                    json.getString("Note"),
                    json.getString("HourCaption"),
                    json.getString("LessonLabel")
                )

                list.add(item)
            }

            return list
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
}