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

package cz.lastaapps.bakalari.app.api.absence

import android.util.Log
import cz.lastaapps.bakalari.app.api.DataIdList
import cz.lastaapps.bakalari.app.api.absence.data.AbsenceDay
import cz.lastaapps.bakalari.app.api.absence.data.AbsenceRoot
import cz.lastaapps.bakalari.app.api.absence.data.AbsenceSubject
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.TimeTools.toCommon
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**Parses absence json*/
object AbsenceParser {

    private val TAG = AbsenceParser::class.java.simpleName

    /**parses json into object structures*/
    fun parseJson(json: JSONObject): AbsenceRoot {

        Log.i(TAG, "Parsing")

        return AbsenceRoot(
            json.getDouble("PercentageThreshold"),
            parseDays(json.getJSONArray("Absences")),
            parseSubjects(json.getJSONArray("AbsencesPerSubject"))
        )
    }

    /**parses AbsenceDay*/
    private fun parseDays(jsonArray: JSONArray): DataIdList<AbsenceDay> {
        val list = DataIdList<AbsenceDay>()

        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(
                AbsenceDay(
                    TimeTools.parse(
                        json.getString("Date"),
                        TimeTools.COMPLETE_FORMAT
                    ).toCommon(),
                    json.getInt("Unsolved"),
                    json.getInt("Ok"),
                    json.getInt("Missed"),
                    json.getInt("Late"),
                    json.getInt("Soon"),
                    json.getInt("School")
                )
            )
        }

        return DataIdList(list.sorted())
    }

    /**parses AbsenceSubject*/
    private fun parseSubjects(jsonArray: JSONArray): DataIdList<AbsenceSubject> {
        val list = DataIdList<AbsenceSubject>()

        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            list.add(
                AbsenceSubject(
                    json.getString("SubjectName"),
                    json.getInt("LessonsCount"),
                    json.getInt("Base"),
                    json.getInt("Late"),
                    json.getInt("Soon"),
                    json.getInt("School")
                )
            )
        }

        return DataIdList(list.sorted())
    }

    /** Tries to get String for the key given, protection again JSONException
     * and replacing null object with "" */
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