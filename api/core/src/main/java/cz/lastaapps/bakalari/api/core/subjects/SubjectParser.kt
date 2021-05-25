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

package cz.lastaapps.bakalari.api.core.subjects

import android.util.Log
import cz.lastaapps.bakalari.api.core.subjects.holders.Subject
import cz.lastaapps.bakalari.api.core.subjects.holders.SubjectTeacherLists
import cz.lastaapps.bakalari.api.core.subjects.holders.Teacher
import org.json.JSONException
import org.json.JSONObject

/**decodes Theme json*/
object SubjectParser {
    private val TAG = SubjectParser::class.java.simpleName

    /**Parses subjects from json, scheme on https://github.com/bakalari-api/bakalari-api-v3*/
    fun parseJson(root: JSONObject): SubjectTeacherLists {

        Log.i(TAG, "Parsing subjects json")

        //parses whole json
        val jsonArray = root.getJSONArray("Subjects")

        val sList = cz.lastaapps.bakalari.api.core.subjects.holders.SubjectList()
        val tList = HashSet<Teacher>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                val teacher = Teacher(
                    json.getString("TeacherID"),
                    json.getString("TeacherName"),
                    json.getString("TeacherAbbrev"),
                    safeJson(json, "TeacherEmail"),
                    safeJson(json, "TeacherWeb"),
                    safeJson(json, "TeacherSchoolPhone"),
                    safeJson(json, "TeacherHomePhone"),
                    safeJson(json, "TeacherMobilePhone")
                )

                tList.add(teacher)

                val subject = Subject(
                    json.getString("SubjectID"),
                    json.getString("SubjectName"),
                    json.getString("SubjectAbbrev"),
                    teacher.id
                )

                sList.add(subject)
            }

        return SubjectTeacherLists(
            sList,
            cz.lastaapps.bakalari.api.core.subjects.holders.TeacherList(tList)
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
